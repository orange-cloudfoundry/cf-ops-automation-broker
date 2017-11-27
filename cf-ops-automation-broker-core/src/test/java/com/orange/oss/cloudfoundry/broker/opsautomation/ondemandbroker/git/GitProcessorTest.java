package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.fest.assertions.Assertions.assertThat;

public class GitProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(GitProcessorTest.class);
    private static final String GIT_URL = "git://127.0.0.1:9418/volatile-repo.git";

    static GitServer gitServer;

    GitProcessor processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");


    @BeforeClass
    public static void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();
        gitServer.startLocalEmptyReposServer(GitServer.NO_OP_INITIALIZER);
    }

    @AfterClass
    public static void stopGitServer() throws InterruptedException {
        gitServer.stopLocalEmptyReposServer();
    }

    @After
    public void cleanUpClone() throws IOException {
        if (processor != null) {
            processor.deleteWorkingDir();
        }
        gitServer.cleanUpRepos();
    }

    @Test
    public void noop_when_no_changes_made() throws Exception {
        //given a clone of an empty repo
        processor.cloneRepo(new Context());
        Iterable<RevCommit> initialCommits = processor.getGit().log().call();

        //when no changes are made
        processor.commitPushRepo(new Context(), false);

        //then repo does not contain new commits
        Iterable<RevCommit> resultingCommits = processor.getGit().log().call();
        assertThat(countIterables(resultingCommits)).isEqualTo(countIterables(initialCommits));
    }

    @Test
    public void configures_user_name_and_email_in_commits() throws GitAPIException, IOException {
        //given explicit user config
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, "committerName", "committer@address.org");
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(new Context(), false);

        //then commit have proper identity
        Iterable<RevCommit> resultingCommits = processor.getGit().log().call();
        RevCommit commit = resultingCommits.iterator().next();
        PersonIdent committerIdent = commit.getCommitterIdent();
        assertThat(committerIdent.getName()).isEqualTo("committerName");
        assertThat(committerIdent.getEmailAddress()).isEqualTo("committer@address.org");
        PersonIdent authorIdent = commit.getAuthorIdent();
        assertThat(authorIdent.getName()).isEqualTo("committerName");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("committer@address.org");
    }

    @Test
    public void supports_default_user_name_and_emails_config() throws GitAPIException, IOException {
        //given no user config specified (null values)
        processor = new GitProcessor("gituser", "gitsecret", GIT_URL, null, null);
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(new Context(), true);

        //then commit does not fail
        //we don't asser the content since this depends on the local execution environment
    }


    @Test
    public void adds_and_deletes_files() throws GitAPIException, IOException {
        //given a clone of an empty repo
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        addAFile(ctx);
        processor.commitPushRepo(new Context(), true);

        //then file should be persisted
        Path secondClone = cloneRepo(processor);
        File secondCloneFile = secondClone.resolve("afile.txt").toFile();
        assertThat(secondCloneFile).exists();

        //when deleting file
        assertThat(secondCloneFile.delete()).isTrue();
        //and committing
        processor.commitPushRepo(new Context(), true);

        //then file should be removed from repo
        Path thirdClone = cloneRepo(processor);
        File thirdCloneFile = thirdClone.resolve("afile.txt").toFile();
        assertThat(thirdCloneFile).doesNotExist();
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

        Iterable<RevCommit> resultingCommits = processor.getGit().log().call();
        RevCommit commit = resultingCommits.iterator().next();
        assertThat(commit.getShortMessage()).isEqualTo("a custom message");
    }

    public void addAFile(Context ctx) throws IOException {
        Path workDir = (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
        try (FileWriter writer = new FileWriter(workDir.resolve("afile.txt").toFile())) {
            writer.append("hello.txt");
        }
    }

    public int countIterables(Iterable<RevCommit> resultingCommits) {
        int size = 0;
        for (RevCommit ignored : resultingCommits) {
            size++;
        }
        return size;
    }

    public Path cloneRepo(GitProcessor processor) {
        Context ctx = new Context();
        processor.preCreate(ctx);

        return (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
    }


}
