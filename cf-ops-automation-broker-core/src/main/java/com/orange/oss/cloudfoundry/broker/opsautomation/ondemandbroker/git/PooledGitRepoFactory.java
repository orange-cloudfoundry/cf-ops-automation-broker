package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.eclipse.jgit.api.Git;

public class PooledGitRepoFactory implements KeyedPooledObjectFactory<Context, Git> {
    private GitManager gitManager;

    PooledGitRepoFactory(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    @Override
    public PooledObject<Git> makeObject(Context key) {
        Git repo = gitManager.cloneRepo(key);
        return new DefaultPooledObject<>(repo);
    }

    @Override
    public void destroyObject(Context key, PooledObject<Git> p) {
        gitManager.deleteWorkingDir(key);
    }

    @Override
    public boolean validateObject(Context key, PooledObject<Git> p) {
        return false;
    }

    @Override
    public void activateObject(Context key, PooledObject<Git> p) {

    }

    @Override
    public void passivateObject(Context key, PooledObject<Git> p) {

    }
}
