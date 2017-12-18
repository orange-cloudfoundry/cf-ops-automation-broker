package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.fest.assertions.Assertions.assertThat;

public class GitProcessorTest {

    private static final String GIT_URL = "git://127.0.0.1:9418/volatile-repo.git";

    static GitServer gitServer;

    GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
    Context ctx = new Context();



    @BeforeClass
    public static void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();
        gitServer.startEphemeralReposServer(GitServer.NO_OP_INITIALIZER);
    }

    @AfterClass
    public static void stopGitServer() throws InterruptedException {
        gitServer.stopAndCleanupReposServer();
    }

    @After
    public void cleanUpClone() throws IOException {
        if (processor != null) {
            processor.deleteWorkingDir(ctx);
        }
        gitServer.cleanUpRepos();
    }

    @Test
    public void noop_when_no_changes_made() throws Exception {
        //given a clone of an empty repo
        processor.cloneRepo(ctx);
        Iterable<RevCommit> initialCommits = processor.getGit(ctx).log().call();

        //when no changes are made
        processor.commitPushRepo(ctx, false);

        //then repo does not contain new commits
        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        assertThat(countIterables(resultingCommits)).isEqualTo(countIterables(initialCommits));
    }

    @Test
    public void configures_user_name_and_email_in_commits() throws GitAPIException, IOException {
        //given explicit user config
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(ctx, false);

        //then commit have proper identity
        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        PersonIdent committerIdent = commit.getCommitterIdent();
        assertThat(committerIdent.getName()).isEqualTo("committerName");
        assertThat(committerIdent.getEmailAddress()).isEqualTo("committer@address.org");
        PersonIdent authorIdent = commit.getAuthorIdent();
        assertThat(authorIdent.getName()).isEqualTo("committerName");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("committer@address.org");
    }

    @Test
    public void supports_createBranchIfMissing_key() throws IOException, GitAPIException {
        //given a clone of an empty repo on the master branch

        GitProcessor processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        Context context = new Context();
        processor1.cloneRepo(context);

        //when adding files
        //and asking to commit and push
        addAFile(context, "hello.txt", "afile-in-" + "develop" + "-branch.txt", "");
        context.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "develop");
        processor1.commitPushRepo(context, false);

        //then file should be persisted
        Context ctx1 = new Context();
        processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        context.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        processor1.preCreate(ctx1);
        Path secondClone = getWorkDir(ctx1, "");
        File secondCloneSameFile = secondClone.resolve("afile-in-" + "develop" + "-branch.txt").toFile();
        assertThat(secondCloneSameFile).exists();
    }

    @Test
    public void supports_checkOutRemoteBranch_key() throws IOException, GitAPIException {
        //given two independent branches available in a remote
        givenAnExistingRepoOnSpecifiedBranch("develop");
        givenAnExistingRepoOnSpecifiedBranch("service-instance-guid");
        givenAnExistingRepoOnSpecifiedBranch("service-instance-guid2");

        //given a clone of develop branch
        this.processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        this.ctx = new Context();
        this.ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        //given instruction in context to create the "service-instance-guid" branch if missing
        this.ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        processor.cloneRepo(this.ctx);

        //and adding files
        //and asking to commit and push
        addAFile(this.ctx, "content in branch service-instance-guid", "another-file-in-service-instance-guid-branch.txt", "");
        processor.commitPushRepo(this.ctx, false);

        Path secondClone = cloneRepoFromBranch("service-instance-guid");
        //then file should be persisted in the "service-instance-guid" branch
        assertThat(secondClone.resolve("afile-in-service-instance-guid-branch.txt").toFile()).exists();
        assertThat(secondClone.resolve("another-file-in-service-instance-guid-branch.txt").toFile()).exists();
        //then the file from the independent develop branch is irrelevant and not present
        assertThat(secondClone.resolve("afile-in-develop-branch.txt").toFile()).exists();
        assertThat(secondClone.resolve("afile-in-service-instance-guid2-branch.txt").toFile()).doesNotExist();
    }

    protected void givenAnExistingRepoOnSpecifiedBranch(String branch) throws IOException, GitAPIException {
        //given a clone of an empty repo on the master branch
        GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        Context context = new Context();
        context.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), branch);
        processor.cloneRepo(context);

        //when adding files
        //and asking to commit and push
        addAFile(context, "hello.txt", "afile-in-" + branch + "-branch.txt", "");
        processor.commitPushRepo(context, false);

        //then file should be persisted in the branch
        Context ctx1 = new Context();
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        ctx1.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), branch);
        processor.preCreate(ctx1);
        Path secondClone = getWorkDir(ctx1, "");
        File secondCloneSameFile = secondClone.resolve("afile-in-" + branch + "-branch.txt").toFile();
        assertThat(secondCloneSameFile).exists();
    }

    @Test
    public void supports_default_user_name_and_emails_config() throws GitAPIException, IOException {
        //given no user config specified (null values)
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, null, null, null);
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(ctx, true);

        //then commit does not fail
        //we don't asser the content since this depends on the local execution environment
    }


    @Test
    public void adds_and_deletes_files() throws GitAPIException, IOException {
        addAndDeleteFilesForRepoAlias(this.processor, this.ctx, "");
    }

    @Test
    public void reads_context_key_with_repo_alias() {
        //given a processor with an alias specified
        GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", "paasSecret");

        //then workdir uses alias as a prefix
        assertThat(processor.getContextKey(GitProcessorContext.workDir)).isEqualTo("paasSecret" + GitProcessorContext.workDir.toString());
    }

    @Test
    public void reads_context_key_without_repo_alias() {
        //given a processor without an alias specified
        GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        //then workdir works as default
        assertThat(processor.getContextKey(GitProcessorContext.workDir)).isEqualTo(GitProcessorContext.workDir.toString());
    }

    @Test
    public void supports_multiple_processors_in_same_context_keys_prefixed_by_a_repo_alias() throws IOException {
        //given two processors instanciated in the same processor chain
        GitProcessor paasSecretProcessor = new GitProcessor("gituser", "gitsecret", "git://127.0.0.1:9418/paas-secret.git", "committerName", "committer@address.org", "paasSecret");
        Context sharedContext = new Context();
        GitProcessor paasTemplateProcessor = new GitProcessor("gituser", "gitsecret", "git://127.0.0.1:9418/paas-template.git", "committerName", "committer@address.org", "paasTemplate");

        //then each processors coexist on the same context without overlapping
        addAndDeleteFilesForRepoAlias(paasSecretProcessor, sharedContext, "paasSecret");
        addAndDeleteFilesForRepoAlias(paasTemplateProcessor, sharedContext, "paasTemplate");
    }

    @Test
    public void rebases_during_push_conflicts() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        GitProcessor processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor1.cloneRepo(ctx1);

        //Given concurrent commits
        Context ctx2 = new Context();
        GitProcessor processor2 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor2.cloneRepo(ctx2);
        addAFile(ctx2, "content2", "a_first_file.txt", "");
        processor2.commitPushRepo(ctx2, true);

        //when trying to commit and push
        addAFile(ctx1, "content1", "a_second_file.txt", "");
        processor1.commitPushRepo(ctx1, true);

        //then a rebase should be tried, a 3rd clone sees both files commited
        Context ctx3 = new Context();
        GitProcessor processor3 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor3.cloneRepo(ctx3);
        Path thirdClone = getWorkDir(ctx3, "");
        assertThat(thirdClone.resolve("a_first_file.txt").toFile()).exists();
        assertThat(thirdClone.resolve("a_second_file.txt").toFile()).exists();

        //cleanup
        processor1.deleteWorkingDir(ctx1);
        processor2.deleteWorkingDir(ctx2);
        processor3.deleteWorkingDir(ctx3);
    }


    @Test
    public void fails_if_pull_rebase_fails_during_push() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        GitProcessor processor1 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor1.cloneRepo(ctx1);

        //Given concurrent commits
        Context ctx2 = new Context();
        GitProcessor processor2 = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor2.cloneRepo(ctx2);
        addAFile(ctx2, "content2", "same_file.txt", "");
        processor2.commitPushRepo(ctx2, true);

        //when trying to commit and push
        addAFile(ctx1, "content1", "same_file.txt", "");
        try {
            processor1.commitPushRepo(ctx1, true);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("conflict");
        }
    }


    @Test
    public void supports_custom_commit_msg() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files with a custom message
        ctx.contextKeys.put(GitProcessorContext.commitMessage.toString(), "a custom message");
        addAFile(ctx);
        processor.commitPushRepo(ctx, false);

        Iterable<RevCommit> resultingCommits = processor.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        assertThat(commit.getShortMessage()).isEqualTo("a custom message");
    }


    ///--------------------------------------- Text fixtures ---------------------------------



    protected Path cloneRepoFromBranch(@SuppressWarnings("SameParameterValue") String branch) {
        GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        Context context = new Context();
        context.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), branch);
        processor.cloneRepo(context);
        return getWorkDir(context, "");
    }

    protected void addAndDeleteFilesForRepoAlias(GitProcessor processor, Context context, String repoAlias) throws IOException {
        //given a clone of an empty repo
        processor.cloneRepo(context);

        //when adding files
        //and asking to commit and push
        addAFile(context, "hello.txt", "afile.txt", repoAlias);
        processor.commitPushRepo(context, true);

        //then file should be persisted
        Context ctx1 = new Context();
        processor.preCreate(ctx1);
        Path secondClone = getWorkDir(ctx1, repoAlias);
        File secondCloneSameFile = secondClone.resolve("afile.txt").toFile();
        assertThat(secondCloneSameFile).exists();

        //when deleting file
        assertThat(secondCloneSameFile.delete()).isTrue();
        //and committing
        processor.commitPushRepo(ctx1, true);

        //then file should be removed from repo
        Path thirdClone = cloneRepo(processor, repoAlias);
        File thirdCloneFile = thirdClone.resolve("afile.txt").toFile();
        assertThat(thirdCloneFile).doesNotExist();

        //cleanup
        processor.deleteWorkingDir(ctx1);
    }

    public void addAFile(Context ctx) throws IOException {
        addAFile(ctx, "hello.txt", "afile.txt", "");
    }

    public void addAFile(Context ctx, String content, String fileRelativePath, String repoAlias) throws IOException {
        Path workDir = getWorkDir(ctx, repoAlias);
        try (FileWriter writer = new FileWriter(workDir.resolve(fileRelativePath).toFile())) {
            writer.append(content);
        }
    }

    public Path getWorkDir(Context ctx, String repoAlias) {
        return (Path) ctx.contextKeys.get(repoAlias + GitProcessorContext.workDir.toString());
    }

    public int countIterables(Iterable<RevCommit> resultingCommits) {
        int size = 0;
        for (RevCommit ignored : resultingCommits) {
            size++;
        }
        return size;
    }

    public Path cloneRepo(GitProcessor processor, String repoAlias) {
        Context ctx = new Context();
        processor.preCreate(ctx);

        return getWorkDir(ctx, repoAlias);
    }


}
