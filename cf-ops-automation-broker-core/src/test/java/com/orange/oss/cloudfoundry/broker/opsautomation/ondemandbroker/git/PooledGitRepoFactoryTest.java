package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.PooledObject;
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PooledGitRepoFactoryTest {

    private GitManager gitManager = Mockito.mock(GitManager.class);
    private Git repo = Mockito.mock(Git.class);
    private PooledGitRepoFactory factory = new PooledGitRepoFactory(gitManager);
    private Context ctx = new Context();

    @Before
    public void setUp() {
        when(gitManager.cloneRepo(any(Context.class))).thenReturn(repo);
    }


    @Test
    public void make_object_delegates_to_git_manager() {
        PooledObject<Git> gitPooledObject = factory.makeObject(ctx);
        assertThat(gitPooledObject.getObject()).isSameAs(repo);
        verify(gitManager).cloneRepo(ctx);
    }

    @Test
    public void destroy_object_deletes_the_git_repo() {
        PooledObject<Git> gitPooledObject = factory.makeObject(ctx);
        factory.destroyObject(ctx, gitPooledObject);
        verify(gitManager).deleteWorkingDir(ctx);
    }

}