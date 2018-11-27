package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.PooledObject;
import org.junit.Test;
import org.mockito.Mockito;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class PooledGitRepoFactoryTest {

    private GitManager gitManager = Mockito.mock(GitManager.class);
    private PooledGitRepoFactory factory = new PooledGitRepoFactory(gitManager);
    private GitPoolKey ctx = ImmutableGitPoolKey.builder().build();

    @Test
    public void makes_object_by_delegating_clones_to_git_manager() {
        PooledObject<Context> gitPooledObject = factory.makeObject(ctx);
        verify(gitManager).cloneRepo(any(Context.class));
        assertThat(gitPooledObject).isNotNull();
    }

    @Test
    public void validate_objects_by_delegating_git_reset_to_git_manager() {
        PooledObject<Context> gitPooledObject = factory.makeObject(ctx);
        boolean valid = factory.validateObject(ctx, gitPooledObject);
        //when
        verify(gitManager).fetchRemoteAndResetCurrentBranch(any(Context.class));
        //then
        assertThat(valid).isTrue();
    }

    @Test
    public void invalidate_objects_when_git_reset_fails() {
        //given
        PooledObject<Context> gitPooledObject = factory.makeObject(ctx);
        //and
        doThrow(new RuntimeException("reset failed")).
                when(gitManager).fetchRemoteAndResetCurrentBranch(any(Context.class));

        //when
        boolean valid = factory.validateObject(ctx, gitPooledObject);
        //then
        assertThat(valid).isFalse();
    }

    @Test
    public void destroy_object_deletes_the_git_repo() {
        PooledObject<Context> gitPooledObject = factory.makeObject(ctx);
        factory.destroyObject(ctx, gitPooledObject);
        verify(gitManager).deleteWorkingDir(any(Context.class));
    }

}