package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PooledGitManagerTest {

    private GitManager gitManager = Mockito.mock(GitManager.class);
    private PooledGitManager pooledGitManager= new PooledGitManager(new PooledGitRepoFactory(gitManager));

    @Test
    public void pools_a_git_repo_across_invocations() {
        //When a 1st clone is pulled and restored to the pool
        Context ctx1 = new Context();
        pooledGitManager.cloneRepo(ctx1);
        //Then the git manager gets delegated the call to clone the repo
        verify(gitManager).cloneRepo(ctx1);
        Mockito.reset(gitManager);

        //When a 1st clone is restored to the pool
        pooledGitManager.deleteWorkingDir(ctx1);
        //And a new clone is requested
        Context ctx2 = new Context();
        pooledGitManager.cloneRepo(ctx2);

        //Then a second clone is NOT created
        verify(gitManager, never()).cloneRepo(ctx1);
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

}