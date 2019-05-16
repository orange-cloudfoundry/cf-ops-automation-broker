package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;

public interface GitManager {

    /**
     * local clone a repo
     *
     * @param ctx exposing the workDir Path in context
     */
    void cloneRepo(Context ctx);

    /**
     * commit, rebase the push the modification
     * @param deleteRepo Delete the repo systematically before returning.
     */
    void commitPushRepo(Context ctx, boolean deleteRepo);

    /**
     * recursively delete working directory
     */
    void deleteWorkingDir(Context ctx);

    void fetchRemoteAndResetCurrentBranch(Context ctxt);
}
