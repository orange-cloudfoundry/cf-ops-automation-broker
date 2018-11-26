package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.nio.file.Path;

/**
 * <pre>
 *     (Processor) Context <---> GitContext <---> (SimpleGitManager) Context
 * </pre>
 *
 * Received Context gets, mapped into a local context (GitContext) with only relevant keys.
 * The local context gets mapped to a SimpleGitManager-scoped Context.
 *
 * The local context gets used as a key to the pool.
 * The SimpleGitManager-scoped Context is used as the value in the pool.
 *
 */
public class PooledGitManager implements GitManager {

    private static final String PRIVATE_POOLED_GIT_CONTEXT = "PrivatePooledGitContext";
    private static final String PRIVATE_LOCAL_GIT_CONTEXT = "PrivateLocalGitContext";
    private KeyedObjectPool<GitContext, Context> pool;
    private String repoAliasName;
    private GitManager gitManager;

    public PooledGitManager(KeyedPooledObjectFactory<GitContext, Context> factory, String repoAliasName, GitManager gitManager) {
        GenericKeyedObjectPoolConfig<Context> poolConfig = constructPoolConfig(repoAliasName);
        pool = new GenericKeyedObjectPool<>(factory, poolConfig);
        this.repoAliasName = repoAliasName;
        this.gitManager = gitManager;
    }

    private GenericKeyedObjectPoolConfig<Context> constructPoolConfig(String repoAliasName) {
        GenericKeyedObjectPoolConfig<Context> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setJmxNamePrefix(repoAliasName);
        return poolConfig;
    }

    @Override
    public void cloneRepo(Context ctx) {
        try {
            GitContext localContext = makeLocalContext(ctx);
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
        gitManager.commitPushRepo(pooledContext,
                false); //don't delete the underling repo that gets pooled instead
    }

    @Override
    public void deleteWorkingDir(Context ctx) {
        Context pooledContext = getPooledContext(ctx);
        GitContext localContext = getLocalContext(ctx);
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

    private void mapOutputResponse(Context ctx, Context pooledContext) {
        Path workDir = (Path) pooledContext.contextKeys.get(repoAliasName + GitProcessorContext.workDir.toString());
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.workDir.toString(), workDir);
    }

    private void saveMappedContexts(Context ctx, Context pooledContext, GitContext local) {
        ctx.contextKeys.put(repoAliasName + PRIVATE_POOLED_GIT_CONTEXT, pooledContext);
        ctx.contextKeys.put(repoAliasName + PRIVATE_LOCAL_GIT_CONTEXT, local);
    }

    private Context getPooledContext(Context ctx) {
        return (Context) ctx.contextKeys.get(repoAliasName + PRIVATE_POOLED_GIT_CONTEXT);
    }
    private GitContext getLocalContext(Context ctx) {
        return (GitContext) ctx.contextKeys.get(repoAliasName + PRIVATE_LOCAL_GIT_CONTEXT);
    }


    GitContext makeLocalContext(Context ctx) {
        ImmutableGitContext.Builder builder = ImmutableGitContext.builder();
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

    public int getActivePlusIdleClones() {
        return pool.getNumActive() + pool.getNumIdle();

    }
}
