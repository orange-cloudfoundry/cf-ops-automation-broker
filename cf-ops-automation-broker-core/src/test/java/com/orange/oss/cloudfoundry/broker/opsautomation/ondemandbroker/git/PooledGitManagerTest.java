package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PooledGitManagerTest {

    private final KeyedPooledObjectFactory<GitPoolKey, Context> factory = createMockedPoolFactory();

    private KeyedPooledObjectFactory<GitPoolKey, Context> createMockedPoolFactory() {
        KeyedPooledObjectFactory<GitPoolKey, Context> mock = mock(KeyedPooledObjectFactory.class);
        try {
            when(mock.makeObject(any())).thenReturn(new DefaultPooledObject(new Context()));
        }
        catch (Exception e) {
            throw new RuntimeException();
        }
        return mock;
    }

    private final String repoAlias = "paas-templates.";
    private final GitManager gitManager = mock(GitManager.class);
    private final Context ctx = new Context();
    private final PooledGitManager pooledGitManager = new PooledGitManager(factory, repoAlias, gitManager, ctx,
        new PoolingProperties());

    @Test
    public void creates_pool_key_from_selected_received_context_request_fields() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        //when
        GitPoolKey gitPoolKey = pooledGitManager.makePoolKey(ctx);
        //then
        assertThat(gitPoolKey.getKeys()).isEqualTo(ctx.contextKeys);
    }

    @Test
    public void creates_pool_key_filtering_out_irrelevant_non_pooleable_fields() {
        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "a msg");
        //when
        GitPoolKey gitPoolKey = pooledGitManager.makePoolKey(ctx);
        //then
        GitPoolKey expectedCtx = ImmutableGitPoolKey.builder().
                putKeys(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop").
                putKeys(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid")
                .build();
        assertThat(gitPoolKey.getKeys()).isEqualTo(expectedCtx.getKeys());
    }

    @Test
    public void copies_non_pooleable_fields() {
        //given
        Context src = new Context();
        src.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        src.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        src.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "a msg");
        //and
        Context dest = new Context();
        dest.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        dest.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        dest.contextKeys.put(repoAlias + GitProcessorContext.workDir, "a path");
        //when
        pooledGitManager.copyNonPooleableEntries(src, dest);
        //then
        Context expected = new Context();
        expected.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        expected.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        expected.contextKeys.put(repoAlias + GitProcessorContext.workDir, "a path");
        expected.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "a msg");
        assertThat(dest.contextKeys).isEqualTo(expected.contextKeys);
    }

    @Test
    public void clears_non_pooleable_fields_after_commit() {
        //given
        Context dest = new Context();
        dest.contextKeys.put(repoAlias + GitProcessorContext.commitMessage, "a msg");
        dest.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        dest.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        dest.contextKeys.put(repoAlias + GitProcessorContext.workDir, "a path");
        //when commit triggers and clean up launched
        pooledGitManager.clearNonPooleableEntries(dest);
        //then
        Context expected = new Context();
        expected.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        expected.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        expected.contextKeys.put(repoAlias + GitProcessorContext.workDir, "a path");
        assertThat(dest.contextKeys).isEqualTo(expected.contextKeys);
    }

    @Test
    public void rejects_pool_key_for_unsupported_submodules_request_fields() {

        //given
        Context ctx = new Context();
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.submoduleListToFetch, Collections.singletonList("mysql-deployment"));

        //when asked to process a request with unsupported attribute
        assertThrows(RuntimeException.class, () -> pooledGitManager.makePoolKey(ctx));
        //then it rejects by throwing an exception
    }


    @Test
    public void clone_exposes_outputs_from_pooled_repo() throws Exception {
        //given
        ctx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        ctx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");

        //and pool returns a context when invoked with return keys
        Context pooledCtx = new Context();
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.checkOutRemoteBranch, "develop");
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.createBranchIfMissing, "service-instance-guid");
        Path gitRepoPath = FileSystems.getDefault().getPath("dummy/path");
        pooledCtx.contextKeys.put(repoAlias + GitProcessorContext.workDir, gitRepoPath);
        when(factory.makeObject(any(GitPoolKey.class))).thenReturn(new DefaultPooledObject<>(pooledCtx));
        when(factory.validateObject(any(GitPoolKey.class), any(PooledObject.class))).thenReturn(true);
        //when asked to clone
        pooledGitManager.cloneRepo(ctx);

        //then returned context contain response values
        Path workDir = (Path) ctx.contextKeys.get(repoAlias + GitProcessorContext.workDir);
        assertThat(workDir).isNotNull();
        assertThat(workDir == gitRepoPath).isTrue();
    }


}