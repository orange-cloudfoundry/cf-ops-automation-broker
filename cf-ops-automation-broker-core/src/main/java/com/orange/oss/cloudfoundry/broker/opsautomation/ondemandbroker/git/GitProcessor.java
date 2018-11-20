package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;


/**
 * Git Mediation : Implements Processor contract by delegating to a GitManager implementation
 */
public class GitProcessor extends DefaultBrokerProcessor {

    private GitManager gitManager;

    public GitProcessor(GitManager gitManager) {
        this.gitManager = gitManager;
    }

    @Override
    public void preCreate(Context ctx) {
        gitManager.cloneRepo(ctx);
    }

    @Override
    public void postCreate(Context ctx) {
        gitManager.commitPushRepo(ctx, true);
    }

    @Override
    public void preGetLastOperation(Context ctx) {
        gitManager.cloneRepo(ctx);
    }

    @Override
    public void postGetLastOperation(Context ctx) {
        gitManager.commitPushRepo(ctx, true);
    }

    @Override
    public void preBind(Context ctx) {
        gitManager.cloneRepo(ctx);
    }

    @Override
    public void postBind(Context ctx) {
        gitManager.commitPushRepo(ctx, true);
    }

    @Override
    public void preDelete(Context ctx) {
        gitManager.cloneRepo(ctx);
    }

    @Override
    public void postDelete(Context ctx) {
        gitManager.commitPushRepo(ctx, true);
    }

    @Override
    public void preUnBind(Context ctx) {
        gitManager.cloneRepo(ctx);
    }

    @Override
    public void postUnBind(Context ctx) {
        gitManager.commitPushRepo(ctx, true);
    }

    @Override
    public void cleanUp(Context ctx) {
        gitManager.deleteWorkingDir(ctx);
    }

}
