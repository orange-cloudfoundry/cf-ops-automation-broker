package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;


/**
 * Git Mediation : Implements Processor contract by delegating to a GitManager implementation
 */
public class GitProcessor extends DefaultBrokerProcessor {

    private GitManager gitManager;
    private String repoAliasName;

    public GitProcessor(GitManager gitManager, String repoAliasName) {
        this.gitManager = gitManager;
        this.repoAliasName = repoAliasName == null ? "" : repoAliasName;
    }

    @Override
    public void preCreate(Context ctx) { cloneRepo(ctx); }

    @Override
    public void postCreate(Context ctx) { commitPushRepo(ctx); }

    @Override
    public void preGetLastOperation(Context ctx) { cloneRepo(ctx); }

    @Override
    public void postGetLastOperation(Context ctx) { commitPushRepo(ctx); }

    @Override
    public void preBind(Context ctx) { cloneRepo(ctx); }

    @Override
    public void postBind(Context ctx) { commitPushRepo(ctx); }

    @Override
    public void preDelete(Context ctx) { cloneRepo(ctx); }

    @Override
    public void postDelete(Context ctx) { commitPushRepo(ctx); }

    @Override
    public void preUnBind(Context ctx) { cloneRepo(ctx); }

    @Override
    public void postUnBind(Context ctx) { commitPushRepo(ctx); }

    @Override
    public void cleanUp(Context ctx) { deleteWorkingDir(ctx);
    }

    private void cloneRepo(Context ctx) {
        if (skipRequest(ctx)) return;
        gitManager.cloneRepo(ctx);
    }

    private void deleteWorkingDir(Context ctx) {
        if (skipRequest(ctx)) return;
        gitManager.deleteWorkingDir(ctx);
    }

    private void commitPushRepo(Context ctx) {
        if (skipRequest(ctx)) return;
        gitManager.commitPushRepo(ctx, true);
    }

    private boolean skipRequest(Context ctx) {
        return getContextValue(ctx, GitProcessorContext.ignoreStep) != null;
    }

    private String getContextValue(Context ctx, GitProcessorContext key) {
        return (String) ctx.contextKeys.get(getContextKey(key));
    }

    private String getContextKey(GitProcessorContext keyEnum) {
        return repoAliasName + keyEnum.toString();
    }

}
