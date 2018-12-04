package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GitProcessorTest {

    private GitManager gitManager = mock(GitManager.class);
    private final String repoAliasName = "paas-templates.";
    private GitProcessor gitProcessor = new GitProcessor(gitManager, repoAliasName);

    @Test
    public void ignore_requests_when_asked_to_in_context() {
        //given a normal request
        Context ctx = new Context();
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        //and a request to ignore git actions
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.ignoreStep, "true");
        //when
        gitProcessor.preCreate(ctx);
        //then
        verify(gitManager, never()).cloneRepo(any(Context.class));

    }
    @Test
    public void delegates_normal_requests_to_gitmanager() {
        //given a normal request
        Context ctx = new Context();
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAliasName + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        //when
        gitProcessor.preCreate(ctx);
        //then
        verify(gitManager).cloneRepo(any(Context.class));
    }

}