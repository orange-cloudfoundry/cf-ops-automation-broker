package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;

/**
 * <pre>
 *     (Processor) Context <---> GitPoolKey <---> (SimpleGitManager) Context
 * </pre>
 *
 * Received Context gets, mapped into a local context (GitPoolKey) with only relevant keys.
 * The local context gets mapped to a SimpleGitManager-scoped Context.
 *
 * The local context gets used as a key to the pool.
 * The SimpleGitManager-scoped Context is used as the value in the pool.
 *
 */
public class PooledGitManager implements GitManager {

    private static final String PRIVATE_POOLED_GIT_CONTEXT = "PrivatePooledGitContext";
    private static final String PRIVATE_LOCAL_GIT_CONTEXT = "PrivateLocalGitContext";
    private KeyedObjectPool<GitPoolKey, Context> pool;
    private String repoAliasName;
    private GitManager gitManager;
    private static final Logger logger = LoggerFactory.getLogger(PooledGitManager.class.getName());


    public PooledGitManager(KeyedPooledObjectFactory<GitPoolKey, Context> factory, String repoAliasName, GitManager gitManager) {
        this.repoAliasName = repoAliasName;
        this.gitManager = gitManager;
        GenericKeyedObjectPoolConfig<Context> poolConfig = constructPoolConfig(repoAliasName);
        pool = new GenericKeyedObjectPool<>(factory, poolConfig);
    }

    private GenericKeyedObjectPoolConfig<Context> constructPoolConfig(String repoAliasName) {
        GenericKeyedObjectPoolConfig<Context> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setJmxNamePrefix(repoAliasName);
        return poolConfig;
    }

    @Override
    public void cloneRepo(Context ctx) {
        logger.debug("Pool with alias {} asked to clone repo with keys {}", repoAliasName, ctx.contextKeys);
        try {
            GitPoolKey localContext = makePoolKey(ctx);
            Context pooledContext = pool.borrowObject(localContext);
            mapOutputResponse(ctx, pooledContext);
            saveMappedContexts(ctx, pooledContext, localContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commitPushRepo(Context ctx, boolean deleteRepo) {
        Context pooledContext = getPooledContext(ctx);
        copyNonPooleableEntries(ctx, pooledContext);
        gitManager.commitPushRepo(pooledContext,
                false); //don't delete the underling repo that gets pooled instead
        clearNonPooleableEntries(pooledContext);
    }

    @Override
    public void deleteWorkingDir(Context ctx) {
        logger.debug("Pool with alias {} asked to release repo with keys {}", repoAliasName, ctx.contextKeys);
        Context pooledContext = getPooledContext(ctx);
        GitPoolKey localContext = getLocalContext(ctx);
        try {
            pool.returnObject(localContext, pooledContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fetchRemoteAndResetCurrentBranch(Context ctxt) {
        throw new IllegalArgumentException("Not supported in pooled impl. Would be the sign of an infinite recursive call");
    }

    public Long getPoolAttribute(Metric metric)  {
        //See https://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html for object name syntax
        //and possibly https://www.oracle.com/technetwork/java/javase/tech/best-practices-jsp-136021.html
        try {
            return (Long) ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("org.apache.commons.pool2" + ":type=GenericKeyedObjectPool,name=" + this.repoAliasName), metric.toString() + "Count");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mapOutputResponse(Context ctx, Context pooledContext) {
        Path workDir = (Path) pooledContext.contextKeys.get(repoAliasName + GitProcessorContext.workDir.toString());
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.workDir.toString(), workDir);
    }

    private void saveMappedContexts(Context ctx, Context pooledContext, GitPoolKey local) {
        ctx.contextKeys.put(repoAliasName + PRIVATE_POOLED_GIT_CONTEXT, pooledContext);
        ctx.contextKeys.put(repoAliasName + PRIVATE_LOCAL_GIT_CONTEXT, local);
    }

    private Context getPooledContext(Context ctx) {
        return (Context) ctx.contextKeys.get(repoAliasName + PRIVATE_POOLED_GIT_CONTEXT);
    }
    private GitPoolKey getLocalContext(Context ctx) {
        return (GitPoolKey) ctx.contextKeys.get(repoAliasName + PRIVATE_LOCAL_GIT_CONTEXT);
    }


    GitPoolKey makePoolKey(Context ctx) {
        ImmutableGitPoolKey.Builder builder = ImmutableGitPoolKey.builder();
        for (GitProcessorContext contextKey : GitProcessorContext.values()) {
            String contextKeyString = repoAliasName + contextKey.toString();
            Object contextValue= ctx.contextKeys.get(contextKeyString);
            if (contextValue == null) {
                continue;
            }
            if (contextKey.isRejectWhenPooled()) {
                throw new IllegalArgumentException("Git Key is not supported when git pooling is enabled:" + contextKey);
            }
            if (contextKey.isPoolable()) {
                builder.putKeys(contextKeyString, contextValue);
            }
        }

        return builder.build();
    }

    @SuppressWarnings("WeakerAccess")
    public void resetMetrics() {
        try {
            pool.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void copyNonPooleableEntries(Context src, Context dest) {
        for (GitProcessorContext contextKey : GitProcessorContext.values()) {
            String contextKeyString = repoAliasName + contextKey.toString();
            Object contextValue= src.contextKeys.get(contextKeyString);
            if (contextValue == null) {
                continue;
            }
            if (!contextKey.isPoolable()) {
                dest.contextKeys.put(contextKeyString, contextValue);
            }
        }
    }
    private void clearNonPooleableEntries(Context ctx) {
        for (GitProcessorContext contextKey : GitProcessorContext.values()) {
            String contextKeyString = repoAliasName + contextKey.toString();
            Object contextValue= ctx.contextKeys.get(contextKeyString);
            if (contextValue == null) {
                continue;
            }
            if (!contextKey.isPoolable()) {
                ctx.contextKeys.remove(contextKeyString);
            }
        }
    }

    public enum Metric {
        Created,
        Borrowed,
        Destroyed,
        Returned

    }

}
