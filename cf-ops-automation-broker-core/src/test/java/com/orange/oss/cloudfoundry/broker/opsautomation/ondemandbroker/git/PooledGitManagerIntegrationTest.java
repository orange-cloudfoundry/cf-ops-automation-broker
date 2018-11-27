package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitManager.Metric.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PooledGitManagerIntegrationTest {
    private String repoAlias = "paas-templates.";
    private GitManager gitManager = Mockito.mock(GitManager.class);

    @Test
    public void pools_a_git_repo_across_invocations() {
        pools_a_git_repo_across_invocations("");
    }

    private PooledGitManager pools_a_git_repo_across_invocations(String repoAliasName) {
        //When a 1st clone is pulled and restored to the pool
        Context ctx1 = new Context();
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAliasName, gitManager);
        pooledGitManager.cloneRepo(ctx1);
        //Then the git manager gets delegated the call to clone the repo
        verify(gitManager, times(1)).cloneRepo(any(Context.class));

        //When a 1st clone is restored to the pool
        pooledGitManager.deleteWorkingDir(ctx1);
        //And a new clone is requested
        Context ctx2 = new Context();
        pooledGitManager.cloneRepo(ctx2);

        //Then a second clone is NOT created
        verify(gitManager, times(1)).cloneRepo(any(Context.class));

        return pooledGitManager;
    }

    @Test
    public void commit_pushes_through_pooled_context_without_asking_repo_deletion() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        //and
        PooledGitManager pooledGitManager= new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAlias, gitManager);

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
    @Ignore("Only gathering ideas/specs for now")
    public void caches_git_clones() {
        //Given an existing repo
        //when a clone is requested
        //then a clone is stored on local disk with a "clone" prefix
        //when the clone is cleaned up
        //it gets renamed with a "cache" prefix

        //Given the repo gets pushed some new modifs

        //when a new clone is requested
        //the cached clone is renamed with a "clone" prefix
        //the clone get fetched the repo content, and reset to the requested branch
    }

    @Test
    public void exposes_empty_pool_metrics_when_no_activity() {
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), "another-yet-unused-pool", gitManager);

        assertThat((long) pooledGitManager.getPoolAttribute(Created)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Borrowed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Destroyed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Returned)).isEqualTo(0);
    }

    @Test(expected=RuntimeException.class)
    public void resets_pool_metrics_upon_request() {
        //given
        PooledGitManager pooledGitManager = pools_a_git_repo_across_invocations("pool-reset-test");
        //when
        pooledGitManager.resetMetrics();
        //then: metric is missing from JMX
        pooledGitManager.getPoolAttribute(Created);
    }

    @Test
    public void exposes_pool_metrics() {
        PooledGitManager pooledGitManager = pools_a_git_repo_across_invocations("unique-id-across-tests-in-jvm");

        assertThat((long) pooledGitManager.getPoolAttribute(Created)).isEqualTo(1);
        assertThat((long) pooledGitManager.getPoolAttribute(Borrowed)).isEqualTo(2);
        assertThat((long) pooledGitManager.getPoolAttribute(Destroyed)).isEqualTo(0);
        assertThat((long) pooledGitManager.getPoolAttribute(Returned)).isEqualTo(1); //we don't return the 2nd clone to the pool
    }


}