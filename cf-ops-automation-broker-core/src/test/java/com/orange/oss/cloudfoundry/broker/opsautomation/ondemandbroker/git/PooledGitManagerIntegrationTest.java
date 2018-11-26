package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.management.*;
import java.lang.management.ManagementFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PooledGitManagerIntegrationTest {
    private String repoAlias = "paas-templates.";

    private GitManager gitManager = Mockito.mock(GitManager.class);
    private PooledGitManager pooledGitManager= new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAlias, gitManager);

    @Test
    public void pools_a_git_repo_across_invocations() {
        pools_a_git_repo_across_invocations("");
    }

    private void pools_a_git_repo_across_invocations(String repoAliasName) {
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
    }

    @Test
    public void commit_pushes_through_pooled_context_without_asking_repo_deletion() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        //and
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
    public void exposes_empty_pool_metrics_when_no_activity() throws Exception {
        String repoAlias = "another-yet-unused-pool";
        PooledGitManager pooledGitManager = new PooledGitManager(new PooledGitRepoFactory(gitManager), repoAlias, gitManager);

        long createdCount = getPoolAttribute("Created", repoAlias);
        long borrowedCount = getPoolAttribute("Borrowed", repoAlias);
        long destroyedCount = getPoolAttribute("Destroyed", repoAlias);
        long returnedCount = getPoolAttribute("Returned", repoAlias);

        assertThat(createdCount).isEqualTo(0);
        assertThat(borrowedCount).isEqualTo(0);
        assertThat(destroyedCount).isEqualTo(0);
        assertThat(returnedCount).isEqualTo(0);
    }

    @Test
    public void exposes_pool_metrics() throws Exception {
        String repoAlias = "unique-id-across-tests-in-jvm";
        pools_a_git_repo_across_invocations(repoAlias);
        long createdCount = getPoolAttribute("Created", repoAlias);
        long borrowedCount = getPoolAttribute("Borrowed", repoAlias);
        long destroyedCount = getPoolAttribute("Destroyed", repoAlias);
        long returnedCount = getPoolAttribute("Returned", repoAlias);

        assertThat(createdCount).isEqualTo(1);
        assertThat(borrowedCount).isEqualTo(2);
        assertThat(destroyedCount).isEqualTo(0);
        assertThat(returnedCount).isEqualTo(1);
    }

    private Long getPoolAttribute(String created, String poolName) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
        //See https://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html for object name syntax
        //and possibly https://www.oracle.com/technetwork/java/javase/tech/best-practices-jsp-136021.html
        return (Long) ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("org.apache.commons.pool2" + ":type=GenericKeyedObjectPool,name=" + poolName), created + "Count");
    }



}