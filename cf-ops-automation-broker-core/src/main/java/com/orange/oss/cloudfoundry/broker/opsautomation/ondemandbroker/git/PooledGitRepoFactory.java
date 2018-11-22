package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PooledGitRepoFactory implements KeyedPooledObjectFactory<GitContext, Context> {
    private GitManager gitManager;
    private static final Logger logger = LoggerFactory.getLogger(PooledGitRepoFactory.class.getName());

    public PooledGitRepoFactory(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    @Override
    public PooledObject<Context> makeObject(GitContext key) {
        logger.info("Building new git repo");
        Context ctx = makeContext(key);
        gitManager.cloneRepo(ctx);
        return new DefaultPooledObject<>(ctx);
    }

    @Override
    public boolean validateObject(GitContext key, PooledObject<Context> p) {
        logger.info("Validating pooled git repo before reusing it");
        Context context = p.getObject();
        try {
            gitManager.fetchRemoteAndResetCurrentBranch(context);
        } catch (Exception e) {
            logger.warn("Failed to refresh git repo, invalidating pooled entry. Caught:" + e);
            return false;
        }
        return true;
    }

    @Override
    public void activateObject(GitContext key, PooledObject<Context> p) {

    }
    @Override
    public void passivateObject(GitContext key, PooledObject<Context> p) {

    }

    @Override
    public void destroyObject(GitContext key, PooledObject<Context> p) {
        gitManager.deleteWorkingDir(p.getObject());
    }


    private Context makeContext(GitContext key) {
        Context ctx = new Context();
        ctx.contextKeys.putAll(key.getKeys());
        return ctx;
    }
}
