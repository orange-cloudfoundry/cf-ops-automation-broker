package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.eclipse.jgit.api.Git;

public class PooledGitManager implements GitManager {

    private KeyedObjectPool<Context, Git> pool;

    PooledGitManager(KeyedPooledObjectFactory<Context, Git> factory) {
        pool = new GenericKeyedObjectPool<>(factory);
    }

    @Override
    public Git cloneRepo(Context ctx) {
        try {
            return pool.borrowObject(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commitPushRepo(Context ctx, boolean deleteRepo) {

    }

    @Override
    public void deleteWorkingDir(Context ctx) {
        try {
            pool.returnObject(ctx, null); //Underlying GitManager will lookup git repo in Context and will ignore null param
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
