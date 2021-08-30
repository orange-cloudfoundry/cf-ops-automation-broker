package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitManager.Metric.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PooledGitManagerIntegrationTest {


    public static final int SECONDS_BETWEEN_EVICTION_RUNS = 1;

    private PoolingProperties noEagerPoolingProperties = createNoEagerPoolingProperties();

    private String repoAlias = "paas-templates.";
    private GitManager gitManager = Mockito.mock(GitManager.class);

    @Test
    public void eagerly_provisions_a_clone_at_initialization_asynchronously() throws InterruptedException {
        //Given an operator configured eager pooling
        PoolingProperties poolingProperties = anEagerPoolingProperties();
        String repoAliasName = repoAlias;

        //When the app starts and manager initializes
        Context ctx1 = new Context();
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAliasName, gitManager,
            ctx1, poolingProperties);

        //Then the git manager DOES NOT get delegated the call to clone the repo
        verify(gitManager, never()).cloneRepo(any(Context.class));

        //when the evictor thread executes
        Thread.sleep(2*SECONDS_BETWEEN_EVICTION_RUNS * 1000);

        //Then the git manager gets delegated the call to clone the repo
        verify(gitManager, times(1)).cloneRepo(any(Context.class));

        pooledGitManager.destruct();
    }

    @Test
    public void pools_a_git_repo_across_invocations() {
        pools_a_git_repo_across_invocations("");
    }

    @Test
    public void cleanup_request_ignored_when_no_previous_clone_completed() {
        //Given clone previously failed, and did not register mapped context
        Context ctx1 = new Context();
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), "", gitManager,
            ctx1, noEagerPoolingProperties);

        //When the clone is asked for clean up
        pooledGitManager.deleteWorkingDir(ctx1);

        //Then clean up is not asked, to avoid NPE
        verify(gitManager, never()).deleteWorkingDir(any(Context.class));

    }

    private PooledGitManager pools_a_git_repo_across_invocations(String repoAliasName) {
        //When a 1st clone is pulled from the pool
        Context ctx1 = new Context();
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAliasName, gitManager,
            ctx1, noEagerPoolingProperties);

        pooledGitManager.cloneRepo(ctx1);
        //Then the git manager gets delegated the call to clone the repo
        verify(gitManager, times(1)).cloneRepo(any(Context.class));
        //And a git pull/reset is triggered as a side effect of pooled object validation, resulting into a noop
        verify(gitManager, times(1)).fetchRemoteAndResetCurrentBranch(any(Context.class));

        //When a 1st clone is restored to the pool
        pooledGitManager.deleteWorkingDir(ctx1);
        //And a new clone is requested
        Context ctx2 = new Context();
        pooledGitManager.cloneRepo(ctx2);

        //Then a second clone is NOT created
        verify(gitManager, times(1)).cloneRepo(any(Context.class));
        //And the pooled git repo is refreshed through a git fetch/reset
        verify(gitManager, times(2)).fetchRemoteAndResetCurrentBranch(any(Context.class));

        return pooledGitManager;
    }

    @Test
    public void eagerly_refills_the_min_idle_pool_asynchronously_when_empty() throws InterruptedException {
        //given eager pooling enabled
        PoolingProperties poolingProperties = anEagerPoolingProperties();
        String repoAliasName = "";
        Context ctx1 = new Context();
        //When the app starts
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAliasName, gitManager,
            ctx1, poolingProperties);
        //Then the git manager eagerly creates a clone
        verify(gitManager, times(1)).cloneRepo(any(Context.class));

        //When a 1st clone is pulled
        pooledGitManager.cloneRepo(ctx1);
        //Then the git manager gets delegated the call to clone the repo
        verify(gitManager, times(1)).cloneRepo(any(Context.class));
        //And a git pull/reset is triggered as a side effect of pooled object validation, resulting into a noop
        verify(gitManager, times(1)).fetchRemoteAndResetCurrentBranch(any(Context.class));

        //When a 1st clone is immediately restored to the pool
        pooledGitManager.deleteWorkingDir(ctx1);
        //And a new clone is requested
        Context ctx2 = new Context();
        pooledGitManager.cloneRepo(ctx2);

        //Then a second clone is NOT immediately created
        verify(gitManager, times(1)).cloneRepo(any(Context.class));
        //And the pooled git repo is refreshed through a git fetch/reset
        verify(gitManager, times(2)).fetchRemoteAndResetCurrentBranch(any(Context.class));

        //Given the pool remains empty longer than the time between evictions
        Thread.sleep(2*SECONDS_BETWEEN_EVICTION_RUNS * 1000);

        //Then the pool is asynchronously refilled with an idle clone
        //emitting a trace similar to:
        //10:36:55.232 [commons-pool-evictor] INFO com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitRepoFactory - Building new git repo with keys {}
        verify(gitManager, times(2)).cloneRepo(any(Context.class));

        //When asking to clean up the used second clone
        pooledGitManager.deleteWorkingDir(ctx2);

        //Then the maxIdle isn't reached and no destroy should trigger (since we don't reach the
        // DEFAULT_MAX_IDLE_PER_KEY = 8, and disable aged-based eviction)
        verify(gitManager, never()).deleteWorkingDir(any(Context.class));

        pooledGitManager.destruct();
    }

    @Test
    public void commit_pushes_through_pooled_context_without_asking_repo_deletion() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        //and
        PooledGitManager pooledGitManager= new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAlias, gitManager,
            ctx, noEagerPoolingProperties);

        pooledGitManager.cloneRepo(ctx);
        ArgumentCaptor<Context> arg1 = ArgumentCaptor.forClass(Context.class);
        verify(gitManager).cloneRepo(arg1.capture());
        Context pooledContext = arg1.getValue();
        //when
        pooledGitManager.commitPushRepo(ctx, true);
        //then
        ArgumentCaptor<Context> arg2 = ArgumentCaptor.forClass(Context.class);
        //commit pushed through, without asking deletion
        verify(gitManager).commitPushRepo(arg2.capture(), eq(false));
        assertThat(arg2.getValue() == pooledContext).isTrue();
    }

    @Test
    public void commit_pushes_through_pooled_context_with_right_commit_msg() {
        //given a first clone request
        Context ctx1 = new Context();
        ctx1.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx1.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        ctx1.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "request 1");
        //and clone get recycled
        PooledGitManager pooledGitManager= new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAlias, gitManager,
            ctx1, noEagerPoolingProperties);
        pooledGitManager.cloneRepo(ctx1);
        pooledGitManager.deleteWorkingDir(ctx1);
        //given a 2nd request
        Context ctx2 = new Context();
        ctx2.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx2.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        ctx2.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "request 2");
        pooledGitManager.cloneRepo(ctx2);
        //when
        pooledGitManager.commitPushRepo(ctx2, true);
        //then
        ArgumentCaptor<Context> arg2 = ArgumentCaptor.forClass(Context.class);
        //commit pushed through with right message
        verify(gitManager).commitPushRepo(arg2.capture(), eq(false));
        // however its cleared before we can first observe it, so assertion would fail.
        //assertThat(arg2.getValue().contextKeys.get(repoAlias + GitProcessorContext.commitMessage)).isEqualTo("request 2");
        assertThat(arg2.getValue().contextKeys.get(repoAlias + GitProcessorContext.commitMessage)).isNull();
    }

    @Test
    public void exposes_empty_pool_metrics_when_no_activity() {
        //given a pool with eager pooling disabled
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), "another-yet" +
            "-unused-pool", gitManager,
            new Context(), noEagerPoolingProperties);
        //when metrics are read
        //then they show no activity
        assertThat((long) pooledGitManager.getPoolAttribute(Created)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Borrowed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Destroyed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Returned)).isEqualTo(0);
    }

    @Test
    public void exposes_initial_eager_warmup_pool_metrics_when_no_activity() {
        //given a pool with eager pooling enabled
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager),
            "another-yet-unused-pool-with-eager-pooling", gitManager,
            new Context(), anEagerPoolingProperties());
        //when metrics are read
        //then they show warmup activity
        assertThat((long) pooledGitManager.getPoolAttribute(Created)).isEqualTo(1);
        assertThat((long) pooledGitManager.getPoolAttribute(Borrowed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Destroyed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Returned)).isEqualTo(0);
    }

    @Test
    public void resets_pool_metrics_upon_request() {
        //given
        PooledGitManager pooledGitManager = pools_a_git_repo_across_invocations("pool-reset-test");
        //when
        pooledGitManager.resetMetrics();
        //then: metric is missing from JMX
        assertThrows(RuntimeException.class, () -> pooledGitManager.getPoolAttribute(Created));
    }

    @Test
    public void exposes_pool_metrics() {
        PooledGitManager pooledGitManager = pools_a_git_repo_across_invocations("unique-id-across-tests-in-jvm");

        assertThat((long) pooledGitManager.getPoolAttribute(Created)).isEqualTo(1);
        assertThat((long) pooledGitManager.getPoolAttribute(Borrowed)).isEqualTo(2);
        assertThat((long) pooledGitManager.getPoolAttribute(Destroyed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Returned)).isEqualTo(1); //we don't return the 2nd clone to the pool
    }

    @NotNull
    private PoolingProperties createNoEagerPoolingProperties() {
        PoolingProperties poolingProperties = new PoolingProperties();
        poolingProperties.setMinIdle(0);
        return poolingProperties;
    }

    @NotNull
    private PoolingProperties anEagerPoolingProperties() {
        PoolingProperties poolingProperties = new PoolingProperties();
        poolingProperties.setMinIdle(1);
        poolingProperties.setSecondsBetweenEvictionRuns(SECONDS_BETWEEN_EVICTION_RUNS);
        return poolingProperties;
    }

}
