package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetrierGitManager implements GitManager {

    private static final Logger logger = LoggerFactory.getLogger(RetrierGitManager.class.getName());
    private GitManager gitManager;
    private RetryPolicy<Object> retryPolicy;

    public RetrierGitManager(String repositoryAliasName, GitManager gitManager, RetryPolicy<Object> retryPolicy) {
        this.gitManager = gitManager;
        this.retryPolicy = retryPolicy;
        this.retryPolicy
                .onRetry(e -> logger.warn("Failure, retrying. Cause: " + e.getLastFailure()))
                .onRetriesExceeded(e -> logger.warn("Aborting. Max retries exceeded:" + this.retryPolicy.getMaxAttempts() + " Rethrowing:" + e.getFailure()))
                .handleIf(e -> isCauseSubclassOf(e, org.eclipse.jgit.api.errors.TransportException.class))
        ;
        logger.debug("Configured for {} with retry policy {}", repositoryAliasName, ToStringBuilder.reflectionToString(retryPolicy));
    }

    protected boolean isCauseSubclassOf(Throwable e, Class superClassToCheck) {
        Throwable cause = e.getCause();
        //noinspection unchecked
        return superClassToCheck.isAssignableFrom(cause.getClass());
    }

    @Override
    public void cloneRepo(Context ctx) {
        Failsafe.with(retryPolicy)
                .run(retryCtx -> {
                    logger.debug("clone attempt #{}", retryCtx.getAttemptCount());
                    gitManager.cloneRepo(ctx);
                });
    }

    @Override
    public void commitPushRepo(Context ctx, boolean deleteRepo) {
        try {
            Failsafe.with(retryPolicy).run(retryCtx -> {
                logger.debug("push attempt #{}", retryCtx.getAttemptCount());
                gitManager.commitPushRepo(ctx, false);
            });
        } finally {
            if (deleteRepo) {
                gitManager.deleteWorkingDir(ctx);
            }
        }
    }

    @Override
    public void deleteWorkingDir(Context ctx) {
        gitManager.deleteWorkingDir(ctx);
    }

    @Override
    public void fetchRemoteAndResetCurrentBranch(Context ctxt) {
        Failsafe.with(retryPolicy).run(retryCtx -> {
            logger.debug("fetch attempt #{}", retryCtx.getAttemptCount());
            gitManager.fetchRemoteAndResetCurrentBranch(ctxt);
        });
    }
}
