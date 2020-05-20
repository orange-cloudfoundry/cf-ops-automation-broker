package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.util.FileSystemUtils;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitIT.createDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimpleGitManagerTest {

    private static final String GIT_BASE_URL = "git://127.0.0.1:9418/";
    private static final String GIT_URL = GIT_BASE_URL + "volatile-repo.git";

    private static GitServer gitServer;

    private SimpleGitManager gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
    private Context ctx = new Context();


    @BeforeAll
    public static void startGitServer() throws IOException {
        gitServer = new GitServer();
        gitServer.startEphemeralReposServer(GitServer.NO_OP_INITIALIZER);
    }

    @AfterAll
    public static void stopGitServer() throws InterruptedException {
        gitServer.stopAndCleanupReposServer();
    }

    @AfterEach
    public void cleanUpClone() {
        if (gitManager != null) {
            gitManager.deleteWorkingDir(ctx);
        }
        gitServer.cleanUpRepos();
    }

    @Test
    public void noop_when_no_changes_made() throws Exception {
        //given a clone of an empty repo
        gitManager.cloneRepo(ctx);
        Iterable<RevCommit> initialCommits = gitManager.getGit(ctx).log().call();

        //when no changes are made
        gitManager.commitPushRepo(ctx, false);

        //then repo does not contain new commits
        Iterable<RevCommit> resultingCommits = gitManager.getGit(ctx).log().call();
        assertThat(countIterables(resultingCommits)).isEqualTo(countIterables(initialCommits));
    }

    @Test
    public void pushes_pending_commits_when_invoked_during_retries() throws Exception {
        //given a clone of an empty repo
        gitManager.cloneRepo(ctx);

        //given a commit is pending and not pushed
        addAFile(ctx, "hello world", "afile.txt", "");
        gitManager.getGit(ctx).add().addFilepattern(".").call();
        gitManager.getGit(ctx).commit().setMessage("a previously added commit, that we expect to be pushed").call();

        //when asking to push in a retry loop
        gitManager.commitPushRepo(ctx, false);

        //then a new clone should have the file
        Context ctx1 = new Context();

        gitManager.cloneRepo(ctx1);
        Path cloneCtxt1 = getWorkDir(ctx1, "");
        File secondCloneSameFile = cloneCtxt1.resolve("afile.txt").toFile();
        assertThat(secondCloneSameFile).exists();
    }

    @Test
    public void detects_pending_commits_during_retries() throws Exception {
        //given a clone of an empty repo
        gitManager.cloneRepo(ctx);

        //given no pending commit
        //then it reports no pending commit
        assertThat(gitManager.hasPendingCommits(gitManager.getGit(ctx), ctx)).isFalse();

        //given a commit is pending and not pushed
        addAFile(ctx, "hello world", "afile.txt", "");
        gitManager.getGit(ctx).add().addFilepattern(".").call();
        gitManager.getGit(ctx).commit().setMessage("a previously added commit, that we expect to be pushed").call();

        //then it reports pending commits
        assertThat(gitManager.hasPendingCommits(gitManager.getGit(ctx), ctx)).isTrue();
    }

    @Test
    public void does_not_try_to_push_when_no_pending_commit_during_retries() throws Exception {
        //given a clone of an empty repo
        gitManager.cloneRepo(ctx);

        //given no commit is pending to push
        //given the git server is stopped
        gitServer.stopServer();

        //when asking to push in a retry loop
        gitManager.commitPushRepo(ctx, false);

        //then it should not have thrown exception trying to push

        //lets restart the server
        gitServer.startServer();
    }

    @Test
    public void configures_user_name_and_email_in_commits() throws GitAPIException, IOException {
        //given explicit user config
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        gitManager.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        gitManager.commitPushRepo(ctx, false);

        //then commit have proper identity
        Iterable<RevCommit> resultingCommits = gitManager.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        PersonIdent committerIdent = commit.getCommitterIdent();
        assertThat(committerIdent.getName()).isEqualTo("committerName");
        assertThat(committerIdent.getEmailAddress()).isEqualTo("committer@address.org");
        PersonIdent authorIdent = commit.getAuthorIdent();
        assertThat(authorIdent.getName()).isEqualTo("committerName");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("committer@address.org");
    }

    @Test
    public void looks_up_existing_remote_branch_by_name() throws IOException, GitAPIException {
        //1- given a repo with a develop branch:

        //given a clone of an empty repo on the master branch
        //and adding files
        //and asking to commit and create missing develop branch
        ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "develop");
        gitManager.cloneRepo(ctx);
        addAFile(ctx, "hello.txt", "afile-in-" + "develop" + "-branch.txt", "");
        gitManager.commitPushRepo(ctx, false);

        //2- when looking up the develop remote branch in a new clone
        Context ctx1 = new Context();
        ctx1.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        gitManager.cloneRepo(ctx1);
        //then it is present
        Optional<Ref> secondLookup = gitManager.lookUpRemoteBranch(gitManager.getGit(ctx1), "develop");
        assertThat(secondLookup.isPresent()).isTrue();
    }

    @Test
    public void supports_createBranchIfMissing_key() throws IOException {
        //given a clone of an empty repo on the "master" branch
        //and adding files, and and asking to commit
        Context context = new Context();
        context.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "develop");
        gitManager.cloneRepo(context);
        addAFile(context, "hello.txt", "afile-in-" + "develop" + "-branch.txt", "");
        // when asking to create a missing "develop" remote branch
        gitManager.commitPushRepo(context, false);


        //then file should be persisted in "develop" branch
        Context ctx1 = new Context();
        ctx1.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        gitManager.cloneRepo(ctx1);
        Path cloneCtxt1 = getWorkDir(ctx1, "");
        File secondCloneSameFile = cloneCtxt1.resolve("afile-in-" + "develop" + "-branch.txt").toFile();
        assertThat(secondCloneSameFile).exists();


        //when adding files
        //and asking to commit and create existing "develop" remote branch
        Context ctx2 = new Context();
        ctx2.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "develop");
        gitManager.cloneRepo(ctx2);
        addAFile(ctx2, "hello.txt", "another-file-in-" + "develop" + "-branch.txt", "");
        //then existing files in existing branch are indeed present in the working dir before push
        //so that we avoid extra polluting rejected push & rebase
        Path cloneCtxt2 = getWorkDir(ctx2, "");
        assertThat(cloneCtxt2.resolve("afile-in-" + "develop" + "-branch.txt").toFile()).exists();
        gitManager.commitPushRepo(ctx2, false);

        //then file should be persisted in the existing develop branch
        Context ctx3 = new Context();
        ctx3.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        gitManager.cloneRepo(ctx3);
        Path thirdClone = getWorkDir(ctx3, "");
        File thirdCloneSameFile = thirdClone.resolve("afile-in-" + "develop" + "-branch.txt").toFile();
        File thirdCloneAnotherFile = thirdClone.resolve("another-file-in-" + "develop" + "-branch.txt").toFile();
        assertThat(thirdCloneSameFile).exists();
        assertThat(thirdCloneAnotherFile).exists();
    }

    @Test
    public void displays_remote_branch_being_pushed_to() {
        Context context0 = new Context();
        assertGetImplicitRemoteBranchToDisplay(context0, "master");

        Context context1 = new Context();
        context1.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        assertGetImplicitRemoteBranchToDisplay(context1, "develop");

        Context context2 = new Context();
        context2.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        context2.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        assertGetImplicitRemoteBranchToDisplay(context2, "service-instance-guid");
    }

    @Test
    public void displays_logs_prefixed_by_the_repo_alias() {
        //given a processor with an alias specified
        SimpleGitManager processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", "paasSecret");

        //when
        String prefixedLog = processor.prefixLog("cloning repo");
        //then
        assertThat(prefixedLog).isEqualTo("[paasSecret] cloning repo");


        //given a processor without an alias specified
        processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        //when
        String nonPrefixedLog = processor.prefixLog("cloning repo");
        //then
        assertThat(nonPrefixedLog).isEqualTo("cloning repo");

    }

    @Test
    public void supports_checkOutRemoteBranch_key_when_branch_exists() throws IOException {
        //given an existing branch in the repo
        givenAnExistingRepoOnSpecifiedBranch("develop");

        //given a clone of develop branch
        this.ctx = new Context();
        this.ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        gitManager.cloneRepo(this.ctx);

        //and adding files
        //and asking to commit and push
        addAFile(this.ctx, "anoter content in branch develop",
                "another-file-in-develop-branch.txt", "");
        gitManager.commitPushRepo(this.ctx, false);

        Path secondClone = cloneRepoFromBranch("develop");
        //then the original file is present
        assertThat(secondClone.resolve("afile-in-develop-branch.txt").toFile()).exists();
        //and the new file should be persisted in the "develop" branch
        assertThat(secondClone.resolve("another-file-in-develop-branch.txt").toFile()).exists();
    }

    @Test
    public void supports_checkOutRemoteBranch_key_when_branch_exists_as_default_branch() throws IOException {
        //given the existing master default branch in the repo

        //given a clone of master branch
        this.ctx = new Context();
        this.ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "master");
        gitManager.cloneRepo(this.ctx);

        //and adding files
        //and asking to commit and push
        addAFile(this.ctx, "some  content in branch master", "a-file-in-master-branch.txt", "");
        gitManager.commitPushRepo(this.ctx, false);

        Path secondClone = cloneRepoFromBranch("master");
        //Then new file should be persisted in the "master" branch
        assertThat(secondClone.resolve("a-file-in-master-branch.txt").toFile()).exists();
    }

    @Test
    public void supports_checkOutRemoteBranch_key_when_branch_is_missing() {
        //given an empty repo

        //When asking to clone develop branch
        this.ctx = new Context();
        this.ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        //then an exception should be thrown
        assertThrows(IllegalArgumentException.class, () ->
            gitManager.cloneRepo(this.ctx));

    }

    @Test
    public void supports_checkOutRemoteBranch_and_createBranchIfMissing_keys_together() throws IOException {
        //given two independent branches available in a remote
        givenAnExistingRepoOnSpecifiedBranch("develop");
        givenAnExistingRepoOnSpecifiedBranch("service-instance-guid2");

        //given a clone of develop branch
        this.ctx = new Context();
        this.ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        //given instruction in context to create a new  "service-instance-guid" branch if missing
        this.ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");
        gitManager.cloneRepo(this.ctx);

        //and adding files
        //and asking to commit and push
        addAFile(this.ctx, "content in branch service-instance-guid", "another-file-in-service-instance-guid-branch.txt", "");
        gitManager.commitPushRepo(this.ctx, true);

        Path secondClone = cloneRepoFromBranch("service-instance-guid");
        //then file should be persisted in the "service-instance-guid" branch
        assertThat(secondClone.resolve("another-file-in-service-instance-guid-branch.txt").toFile()).exists();
        //then previous the file from the base branch is present
        assertThat(secondClone.resolve("afile-in-develop-branch.txt").toFile()).exists();
        //then the file from the independent develop branch is irrelevant and not present
        assertThat(secondClone.resolve("afile-in-service-instance-guid2-branch.txt").toFile()).doesNotExist();

    }

    @Test
    public void supports_default_user_name_and_emails_config() throws IOException {
        //given no user config specified (null values)
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_URL, null, null, null);
        gitManager.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        gitManager.commitPushRepo(ctx, true);

        //then commit does not fail
        //we don't asser the content since this depends on the local execution environment
    }


    @Test
    public void adds_and_deletes_files() throws IOException {
        addAndDeleteFilesForRepoAlias(this.gitManager, this.ctx, "");
    }

    @Test
    public void supports_pooling_with_git_fetch_adds_reset() throws Exception {
        //given an existing repo
        String repoName = "paas-template.git";
        gitServer.initRepo(repoName, this::initPaasTemplate);
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + repoName, "committerName", "committer@address.org", null);
        ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        //and asking to clone it
        gitManager.cloneRepo(ctx);

        //and some new commits gets added to the repo by someone else
        Git git = gitServer.getRepo(repoName);
        git.checkout().setName("develop").call();
        Path gitServerWorkDir = git.getRepository().getDirectory().getParentFile().toPath();
        addAFile("new context", "newFileAfterClone.txt", gitServerWorkDir);
        git.add().addFilepattern("newFileAfterClone.txt").call();
        git.commit().setMessage("dummy msg").call();
        git.close();

        //and the repo is asked to be recycled
        gitManager.fetchRemoteAndResetCurrentBranch(ctx);

        //then the updated file gets fetched
        Path workDir = getWorkDir(ctx, "");
        File fetchedFile = workDir.resolve("newFileAfterClone.txt").toFile();
        assertThat(fetchedFile).exists();

        //Note: ignore debug JGit traces, apparent side effect of git repo test setup
        //org.eclipse.jgit.lib.Repository - close() called when useCnt is already zero for Repository
    }

    @Test
    public void supports_pooling_with_git_fetch_adds_reset_and_create_new_branch() throws Exception {
        //given an existing repo
        String repoName = "paas-template.git";
        gitServer.initRepo(repoName, this::initPaasTemplate);
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + repoName, "committerName", "committer@address.org", null);
        ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        this.ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "service-instance-guid");

        //and asking to clone it
        gitManager.cloneRepo(ctx);

        //and some new commits gets added to the repo by someone else
        Git git = gitServer.getRepo(repoName);
        git.checkout().setName("service-instance-guid").call();
        Path gitServerWorkDir = git.getRepository().getDirectory().getParentFile().toPath();
        addAFile("new context", "newFileAfterClone.txt", gitServerWorkDir);
        git.add().addFilepattern("newFileAfterClone.txt").call();
        git.commit().setMessage("dummy msg").call();
        git.close();

        //and the repo is asked to be recycled
        gitManager.fetchRemoteAndResetCurrentBranch(ctx);

        //then the updated file gets fetched
        Path workDir = getWorkDir(ctx, "");
        File fetchedFile = workDir.resolve("newFileAfterClone.txt").toFile();
        assertThat(fetchedFile).exists();

        //Note: ignore debug JGit traces, apparent side effect of git repo test setup
        //org.eclipse.jgit.lib.Repository - close() called when useCnt is already zero for Repository
    }

    private void addAndDeleteFilesForRepoAlias(SimpleGitManager gitManager, Context context, String repoAlias) throws IOException {
        //given a clone of an empty repo
        gitManager.cloneRepo(context);

        //when adding files
        //and asking to commit and push
        addAFile(context, "hello.txt", "afile.txt", repoAlias);
        gitManager.commitPushRepo(context, true);

        //then file should be persisted
        Context ctx1 = new Context();
        gitManager.cloneRepo(ctx1);
        Path secondClone = getWorkDir(ctx1, repoAlias);
        File secondCloneSameFile = secondClone.resolve("afile.txt").toFile();
        assertThat(secondCloneSameFile).exists();

        //when deleting file
        assertThat(secondCloneSameFile.delete()).isTrue();
        //and committing
        gitManager.commitPushRepo(ctx1, true);

        //then file should be removed from repo
        Path thirdClone = cloneRepo(gitManager, repoAlias);
        File thirdCloneFile = thirdClone.resolve("afile.txt").toFile();
        assertThat(thirdCloneFile).doesNotExist();

        //cleanup
        gitManager.deleteWorkingDir(ctx1);
    }

    @Test
    public void creates_human_readeable_workdir_repo_dir_prefix() {
        String repoWorkDirPrefix;

        //when invoked with a repo alias consistent with what BoshBroker assigns (i.e. with a containing dot)
        repoWorkDirPrefix = gitManager.getRepoWorkDirPrefix("paas-templates.");

        //then
        assertThat(repoWorkDirPrefix).isEqualTo("paas-templates-clone-");

        //when invoked with a repo alias with an invalid char for paths
        repoWorkDirPrefix = gitManager.getRepoWorkDirPrefix("paas-templates/");

        //then it is replaced by dash
        assertThat(repoWorkDirPrefix).isEqualTo("paas-templates-clone-");
    }

    @Test
    public void cleans_up_workdir_on_cleanup_method() {
        //given a clone of an empty repo
        gitManager.cloneRepo(ctx);
        Path workDir = getWorkDir(ctx, "");
        assertThat(workDir.toFile().exists()).isTrue();

        //when
        gitManager.deleteWorkingDir(ctx);

        //then
        assertThat(workDir.toFile().exists()).isFalse();
    }

    @Test
    public void disables_git_linefeed_cleanups_if_any() throws IOException {
        //given
        Path tempDirectory = Files.createTempDirectory("GitProcessorTest_configures_autoclrf_to_false");
        Path boshDeploymentRepo = tempDirectory.resolve("a_repo").resolve(".git");
        Repository repository = FileRepositoryBuilder.create(boshDeploymentRepo.toFile());
        repository.create();

        //given tests run on a developer machine where the global config is
        // "core.autocrlf = input" (which is the common case in the Orange elpaaso team

        //when
        gitManager.configureCrLf(repository.getConfig());

        //then line feedchange are disabled, by setting the false option.

        // https://stackoverflow.com/questions/3206843/how-line-ending-conversions-work-with-git-core-autocrlf-between-different-operat
        // This is the default [...] The result of using false is that Git doesn’t ever
        // mess with line endings on your file. You can check in files with LF or CRLF or
        // CR or some random mix of those three and Git does not care.
        //
        // In other words, COAB does not attempt to clean up invalid CRLF in repos
        // and leaves them as it.
        assertThat(repository.getConfig().getString("core", null, "autocrlf")).isEqualTo("false");

        FileSystemUtils.deleteRecursively(tempDirectory.toFile());
    }

    @Test
    public void fetches_submodules_when_asked() {

        //given a repo with submodules configured
        gitServer.initRepo("bosh-deployment.git", this::initNotEmptyRepo);
        gitServer.initRepo("mysql-deployment.git", this::initNotEmptyRepo);
        gitServer.initRepo("paas-template.git", this::initPaasTemplateWithSubModules);
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + "paas-template.git", "committerName", "committer@address.org", null);
        ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        //when asking to clone it without opt-in for submodules
        gitManager.cloneRepo(ctx);

        //then the submodule isn't fetched (and no exception is thrown)
        Path workDir = getWorkDir(ctx, "");
        assertThat(workDir.resolve("coab-depls").resolve("cassandra").toFile()).exists();
        assertThat(workDir.resolve("bosh-deployment").toFile()).isEmptyDirectory();
        assertThat(workDir.resolve("mysql-deployment").toFile()).isEmptyDirectory();

        //when asking to clone it with opt-in for specific submodules
        ctx.contextKeys.put(GitProcessorContext.submoduleListToFetch.toString(), Collections.singletonList("mysql-deployment"));
        gitManager.cloneRepo(ctx);

        //then the submodule isn't fetched (and no exception is thrown)
        workDir = getWorkDir(ctx, "");
        assertThat(workDir.resolve("coab-depls").resolve("cassandra").toFile()).exists();
        assertThat(workDir.resolve("bosh-deployment").toFile()).isEmptyDirectory();
        assertThat(workDir.resolve("mysql-deployment").toFile()).isDirectory();
        assertThat(workDir.resolve("mysql-deployment").toFile()).isDirectoryRecursivelyContaining("glob:**/a-sub-dir/a-file.txt");

        //when asking to clone with all submodules opted-in
        ctx.contextKeys.clear();
        ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
        ctx.contextKeys.put(GitProcessorContext.fetchAllSubModules.toString(), Boolean.TRUE);
        gitManager.cloneRepo(ctx);

        //then the submodule isn't fetched (and no exception is thrown)
        workDir = getWorkDir(ctx, "");
        assertThat(workDir.resolve("coab-depls").resolve("cassandra").toFile()).exists();
        assertThat(workDir.resolve("bosh-deployment").toFile()).isDirectoryRecursivelyContaining("glob:**/a-sub-dir/a-file.txt");
        assertThat(workDir.resolve("mysql-deployment").toFile()).isDirectoryRecursivelyContaining("glob:**/a-sub-dir/a-file.txt");
    }

    @Test
    public void ignores_fetches_submodules_from_staged_files() {

        //given a repo with submodules configured
        gitServer.initRepo("paas-template.git", this::initPaasTemplateWithSubModules);
        gitManager = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + "paas-template.git", "committerName", "committer@address.org", null);
        ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");

        //when asking to clone it without opt-in for submodules
        gitManager.cloneRepo(ctx);

        //then the list of submodules to ignore from the staging list is pushed to the context
        @SuppressWarnings("unchecked")
        List<String> subModulesList = (List<String>) ctx.contextKeys.get(SimpleGitManager.PRIVATE_SUBMODULES_LIST);
        assertThat(subModulesList).containsOnly("bosh-deployment", "mysql-deployment");

        //so that submodules get excluded from commit list
    }

    public void initNotEmptyRepo(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup #initNotEmptyRepo").call();

            //root deployment
            Path subdir = gitWorkDir.toPath().resolve("a-sub-dir");
            createDir(subdir);
            createDummyFile(subdir.resolve("a-file.txt"));
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            git.commit().setMessage("#initNotEmptyRepo").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createDummyFile(Path path) throws IOException {
        try (Writer writer = new FileWriter(path.toFile())) {
            writer.write("dummy content");
        }
    }

    private void initPaasTemplateWithSubModules(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();

            //In develop branch
            git.checkout().setName("develop").setCreateBranch(true).call();

            //root deployment
            Path coabDepls = gitWorkDir.toPath().resolve("coab-depls");
            createDir(coabDepls);
            //sub deployments
            createDir(coabDepls.resolve("cassandra"));

            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            git.submoduleInit().call();
// attempt to create local submodule to simulate missing submodule:
// Failed due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=467611
//            Path boshDeploymentRepo = gitWorkDir.toPath().resolve("bosh-deployment").resolve(".git");
//            Repository repository = FileRepositoryBuilder.create(boshDeploymentRepo.toFile());
//            repository.create();
            git.submoduleAdd().setPath("bosh-deployment").setURI(GIT_BASE_URL + "bosh-deployment.git").call();
            git.submoduleAdd().setPath("mysql-deployment").setURI(GIT_BASE_URL + "mysql-deployment.git").call();
            git.commit().setMessage("GitIT#startGitServer").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initPaasTemplate(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();

            //In develop branch
            git.checkout().setName("develop").setCreateBranch(true).call();

            //root deployment
            Path coabDepls = gitWorkDir.toPath().resolve("coab-depls");
            createDir(coabDepls);
            //sub deployments
            createDir(coabDepls.resolve("cassandra"));

            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            git.commit().setMessage("GitIT#startGitServer").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void reads_context_key_with_repo_alias() {
        //given a processor with an alias specified
        SimpleGitManager processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", "paasSecret");

        //then workdir uses alias as a prefix
        assertThat(processor.getContextKey(GitProcessorContext.workDir)).isEqualTo("paasSecret" + GitProcessorContext.workDir.toString());
    }

    @Test
    public void reads_context_key_without_repo_alias() {
        //given a processor without an alias specified
        SimpleGitManager processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        //then workdir works as default
        assertThat(processor.getContextKey(GitProcessorContext.workDir)).isEqualTo(GitProcessorContext.workDir.toString());
    }

    @Test
    public void supports_multiple_processors_in_same_context_keys_prefixed_by_a_repo_alias() throws IOException {
        //given two processors instanciated in the same processor chain
        SimpleGitManager paasSecretProcessor = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + "paas-secret.git", "committerName", "committer@address.org", "paasSecret");
        Context sharedContext = new Context();
        SimpleGitManager paasTemplateProcessor = new SimpleGitManager("gituser", "gitsecret", GIT_BASE_URL + "paas-template.git", "committerName", "committer@address.org", "paasTemplate");

        //then each processors coexist on the same context without overlapping
        addAndDeleteFilesForRepoAlias(paasSecretProcessor, sharedContext, "paasSecret");
        addAndDeleteFilesForRepoAlias(paasTemplateProcessor, sharedContext, "paasTemplate");
    }


    @Test
    public void rebases_during_push_conflicts() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        SimpleGitManager processor1 = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor1.cloneRepo(ctx1);
        assertThat(getNbCommits(ctx1, processor1)).describedAs("initial commit").isEqualTo(1);

        //Given concurrent commits
        Context ctx2 = new Context();
        SimpleGitManager processor2 = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor2.cloneRepo(ctx2);
        addAFile(ctx2, "content2", "a_first_file.txt", "");
        processor2.commitPushRepo(ctx2, false);
        assertThat(getNbCommits(ctx2, processor2)).describedAs("initial + 1st file addition").isEqualTo(2);

        //when trying to commit and push
        addAFile(ctx1, "content1", "a_second_file.txt", "");
        processor1.commitPushRepo(ctx1, false);

        //then a rebase should be tried, a 3rd clone sees both files commited
        Context ctx3 = new Context();
        SimpleGitManager processor3 = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor3.cloneRepo(ctx3);
        Path thirdClone = getWorkDir(ctx3, "");
        assertThat(thirdClone.resolve("a_first_file.txt").toFile()).exists();
        assertThat(thirdClone.resolve("a_second_file.txt").toFile()).exists();
        assertThat(getNbCommits(ctx3, processor3)).describedAs("expecting no merge commit").isEqualTo(3);

        //cleanup
        processor1.deleteWorkingDir(ctx1);
        processor2.deleteWorkingDir(ctx2);
        processor3.deleteWorkingDir(ctx3);
    }

    private long getNbCommits(Context ctx, SimpleGitManager processor) throws GitAPIException {
        Git git = processor.getGit(ctx);
        Iterable<RevCommit> commits = git.log().call();
        return StreamSupport.stream(commits.spliterator(), false) //https://stackoverflow.com/questions/23932061/convert-iterable-to-stream-using-java-8-jdk
                .count();
    }


    @Test
    public void fails_if_pull_rebase_fails_during_push() throws IOException {
        //given a clone of an empty repo
        Context ctx1 = new Context();
        SimpleGitManager processor1 = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        processor1.cloneRepo(ctx1);

        //Given concurrent commits
        Context ctx2 = new Context();
        SimpleGitManager processor2 = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
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
        gitManager.cloneRepo(ctx);

        //when adding files with a custom message
        ctx.contextKeys.put(GitProcessorContext.commitMessage.toString(), "a custom message");
        addAFile(ctx);
        gitManager.commitPushRepo(ctx, false);

        Iterable<RevCommit> resultingCommits = gitManager.getGit(ctx).log().call();
        RevCommit commit = resultingCommits.iterator().next();
        assertThat(commit.getShortMessage()).isEqualTo("a custom message");
    }

    private void assertGetImplicitRemoteBranchToDisplay(Context context, String expectedBranchDisplayed) {
        assertThat(gitManager.getImplicitRemoteBranchToDisplay(context)).isEqualTo(expectedBranchDisplayed);
    }

    private void givenAnExistingRepoOnSpecifiedBranch(String branch) throws IOException {
        //given a clone of an empty repo on the master branch
        SimpleGitManager processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);

        Context context = new Context();
        context.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), branch);
        processor.cloneRepo(context);

        //when adding files
        //and asking to commit and push
        addAFile(context, "hello.txt", "afile-in-" + branch + "-branch.txt", "");
        processor.commitPushRepo(context, false);

        //then file should be persisted in the branch
        Context ctx1 = new Context();
        processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        ctx1.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), branch);
        processor.cloneRepo(ctx1);
        Path secondClone = getWorkDir(ctx1, "");
        File secondCloneSameFile = secondClone.resolve("afile-in-" + branch + "-branch.txt").toFile();
        assertThat(secondCloneSameFile).exists();
    }


    ///--------------------------------------- Text fixtures ---------------------------------


    private Path cloneRepoFromBranch(@SuppressWarnings("SameParameterValue") String branch) {
        SimpleGitManager processor = new SimpleGitManager("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org", null);
        Context context = new Context();
        context.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), branch);
        processor.cloneRepo(context);
        return getWorkDir(context, "");
    }

    private void addAFile(Context ctx) throws IOException {
        addAFile(ctx, "hello.txt", "afile.txt", "");
    }

    private void addAFile(Context ctx, String content, String fileRelativePath, String repoAlias) throws IOException {
        Path workDir = getWorkDir(ctx, repoAlias);
        addAFile(content, fileRelativePath, workDir);
    }

    private void addAFile(String content, String fileRelativePath, Path workDir) throws IOException {
        try (FileWriter writer = new FileWriter(workDir.resolve(fileRelativePath).toFile())) {
            writer.append(content);
        }
    }

    private Path getWorkDir(Context ctx, String repoAlias) {
        return (Path) ctx.contextKeys.get(repoAlias + GitProcessorContext.workDir.toString());
    }

    private int countIterables(Iterable<RevCommit> resultingCommits) {
        int size = 0;
        for (RevCommit ignored : resultingCommits) {
            size++;
        }
        return size;
    }

    private Path cloneRepo(SimpleGitManager processor, String repoAlias) {
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        return getWorkDir(ctx, repoAlias);
    }


}
