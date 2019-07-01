package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import net.jodah.failsafe.RetryPolicy;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RetrierGitManagerTest {

    @Mock
    GitManager gitManager;



    @Test
    public void retries_clones_until_max_duration_reached() {
        //Given a retrier is configured with a retry policy and a max retry duration of 2s
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withMaxAttempts(3)
                .withMaxDuration(Duration.ofMillis(2*1000));
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given 2 network problems when trying to clone
        TransportException gitException = new TransportException("https://elpaaso-gitlab.mycompany.com/paas-templates.git: 502 Bad Gateway");
        IllegalArgumentException wrappedException = new IllegalArgumentException(gitException);
        //Inject delay, see https://stackoverflow.com/questions/12813881/can-i-delay-a-stubbed-method-response-with-mockito
        doAnswer( new AnswersWithDelay( 3*1000,  new ThrowsException(wrappedException))). //1st attempt consumming max retry time budget
                doNothing(). //2nd attempt that should not happen
                doNothing(). //3nd attempt that should not happen
                when(gitManager).cloneRepo(any());

        try {
            //when trying to clone
            retrier.cloneRepo(new Context());
            //then it rethrows the exception
            Assertions.fail("expected max attempts reached to rethrow last exception, as max duration exceeded");
        } catch (IllegalArgumentException e) {
            verify(gitManager, times(1)).cloneRepo(any());
            //success
        }

    }

    @Test
    public void retries_clones() {
        //Given a retrier is configured with a retry policy
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxAttempts(3);
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given 2 network problems when trying to clone
        TransportException gitException = new TransportException("https://elpaaso-gitlab.mycompany.com/paas-templates.git: 502 Bad Gateway");
        IllegalArgumentException wrappedException = new IllegalArgumentException(gitException);
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doNothing().           //3rd attempt
                when(gitManager).cloneRepo(any());

        //when trying to clone
        retrier.cloneRepo(new Context());

        //then it retries 3 times and succeeds
        verify(gitManager, times(3)).cloneRepo(any());


        //Given 3 network problems when trying to clone
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doThrow(wrappedException). //2rd attempt
                doNothing().           //4th attempt
                when(gitManager).cloneRepo(any());

        try {
            //when trying to clone
            retrier.cloneRepo(new Context());
            //then it rethrows the exception
            Assertions.fail("expected max attempts reached to rethrow last exception");
        } catch (IllegalArgumentException e) {
            verify(gitManager, times(6)).cloneRepo(any());
            //success
        }

    }

    @Test
    public void fails_fast_on_non_recoverable_exceptions() {
        //Given a retrier is configured with a retry policy
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxAttempts(3);
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given 2 network problems when trying to clone
        Exception nonRecoverableException = new org.eclipse.jgit.api.errors.RefNotFoundException("Ref origin/feature-mongodb-xlarge-plan can not be resolved");
        IllegalArgumentException wrappedException = new IllegalArgumentException(nonRecoverableException);
        doThrow(wrappedException). //1st attempt
                doNothing(). //2nd attempt
                doNothing().           //3rd attempt
                when(gitManager).cloneRepo(any());

        try {
            //when trying to clone
            retrier.cloneRepo(new Context());
            //then it rethrows the exception
            Assertions.fail("expected non recoverable exception to not trigger retry");
        } catch (IllegalArgumentException e) {
            verify(gitManager, times(1)).cloneRepo(any());
            //success
        }
    }
   @Test
    public void cleans_up_after_push_when_asked() {
        //Given a retrier is configured with a retry policy
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxAttempts(3);
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given no network problems when trying to push
       doNothing().           //1st attempt
               when(gitManager).commitPushRepo(any(),anyBoolean());

        //when asked to clean up after push
        retrier.commitPushRepo(new Context(), true);

        //then it does not cleanup
        verify(gitManager, times(1)).commitPushRepo(any(),anyBoolean());
        verify(gitManager, times(1)).deleteWorkingDir(any());

       //when NOT asked to clean up after push
       retrier.commitPushRepo(new Context(), false);

       //then it does NOT cleanup
       verify(gitManager, times(2)).commitPushRepo(any(),anyBoolean());
       verify(gitManager, times(1)).deleteWorkingDir(any());
    }

    @Test
    public void retries_fetches() {
        //Given a retrier is configured with a retry policy
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxAttempts(3);
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given 2 network problems when trying to clone
        TransportException gitException = new TransportException("https://elpaaso-gitlab.mycompany.com/paas-templates.git: 502 Bad Gateway");
        IllegalArgumentException wrappedException = new IllegalArgumentException(gitException);
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doNothing().           //3rd attempt
                when(gitManager).fetchRemoteAndResetCurrentBranch(any());

        //when trying to fetch
        retrier.fetchRemoteAndResetCurrentBranch(new Context());

        //then it retries 3 times and succeeds
        verify(gitManager, times(3)).fetchRemoteAndResetCurrentBranch(any());


        //Given 3 network problems when trying to fetch
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doThrow(wrappedException). //2rd attempt
                doNothing().           //4th attempt
                when(gitManager).fetchRemoteAndResetCurrentBranch(any());

        try {
            //when trying to fetch
            retrier.fetchRemoteAndResetCurrentBranch(new Context());
            //then it rethrows the exception
            Assertions.fail("expected max attempts reached to rethrow last exception");
        } catch (IllegalArgumentException e) {
            verify(gitManager, times(6)).fetchRemoteAndResetCurrentBranch(any());
            //success
        }

    }

    @Test
    public void retries_commit_push() {
        //asking to push without deleting the repo
        //retry: asking to push without deleting the repo
        //success: delete the repo
        //max retry reach: catch exception and delete the repo


        //Given a retrier is configured with a retry policy
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxAttempts(3);
        GitManager retrier = new RetrierGitManager("repoAlias", gitManager, retryPolicy);

        //Given 2 network problems when trying to push
        TransportException gitException = new TransportException("https://elpaaso-gitlab.mycompany.com/paas-templates.git: 502 Bad Gateway");
        IllegalArgumentException wrappedException = new IllegalArgumentException(gitException);
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doNothing().           //3rd attempt
                when(gitManager).commitPushRepo(any(), anyBoolean());

        //when trying to push
        retrier.commitPushRepo(new Context(), true);

        //then it retries 3 times and succeeds
        verify(gitManager, times(3)).commitPushRepo(any(Context.class), eq(false));
        verify(gitManager, times(1)).deleteWorkingDir(any(Context.class));

        //Given 3 network problems when trying to push
        doThrow(wrappedException). //1st attempt
                doThrow(wrappedException). //2nd attempt
                doThrow(wrappedException). //3rd attempt
                doNothing().           //4th attempt
                when(gitManager).commitPushRepo(any(), anyBoolean());

        try {
            //when trying to push
            retrier.commitPushRepo(new Context(), true);
            //then it rethrows the exception
            Assertions.fail("expected max attempts reached to rethrow last exception");
        } catch (IllegalArgumentException e) {
            //success
            verify(gitManager, times(6)).commitPushRepo(any(Context.class), eq(false));
        }
    }

}
