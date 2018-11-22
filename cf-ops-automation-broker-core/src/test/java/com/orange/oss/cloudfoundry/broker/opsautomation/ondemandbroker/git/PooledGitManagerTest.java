package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PooledGitManagerTest {

    @SuppressWarnings("unchecked")
    private KeyedPooledObjectFactory<GitContext, Context> factory = mock(KeyedPooledObjectFactory.class);
    private String repoAlias = "paas-templates.";
    private GitManager gitManager = mock(GitManager.class);
    private PooledGitManager pooledGitManager = new PooledGitManager(factory, repoAlias, gitManager);
    private Context ctx = new Context();

    @Test
    public void creates_pool_key_from_selected_received_context_request_fields() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        //when
        GitContext gitContext = pooledGitManager.makeLocalContext(ctx);
        //then
        assertThat(gitContext.getKeys()).isEqualTo(ctx.contextKeys);
    }

    @Test
    public void creates_pool_key_filtering_out_irrelevant_non_pooleable_fields() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.commitMessage.toString(), "a msg");
        //when
        GitContext gitContext = pooledGitManager.makeLocalContext(ctx);
        //then
        Context expectedCtx = new Context();
        expectedCtx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        expectedCtx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        assertThat(gitContext.getKeys()).isEqualTo(expectedCtx.contextKeys);
    }

    @Test(expected = RuntimeException.class)
    public void rejects_pool_key_for_unsupported_submodules_request_fields() {

        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.submoduleListToFetch.toString(), Collections.singletonList("mysql-deployment"));

        //when asked to process a request with unsupported attribute
        pooledGitManager.makeLocalContext(ctx);
        //then it rejects by throwing an exception
    }


    @Test
    public void clone_exposed_outputs_from_pooled_repo() throws Exception {
        //given
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");

        //and pool returns a context when invoked with return keys
        Context pooledCtx = new Context();
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        Path gitRepoPath = FileSystems.getDefault().getPath("dummy/path");
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.workDir.toString(), gitRepoPath);
        when(factory.makeObject(any(GitContext.class))).thenReturn(new DefaultPooledObject<>(pooledCtx));
        //when asked to clone
        pooledGitManager.cloneRepo(ctx);

        //then returned context contain response values
        Path workDir = (Path) ctx.contextKeys.get(repoAlias + GitProcessorContext.workDir.toString());
        assertThat(workDir).isNotNull();
        assertThat(workDir == gitRepoPath).isTrue();
    }


}