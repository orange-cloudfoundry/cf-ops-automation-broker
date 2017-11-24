package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
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

    @BeforeClass
    public static void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();
        gitServer.startLocalEmptyReposServer();
    }

    @AfterClass
    public static void stopGitServer() throws InterruptedException {
        gitServer.stopLocalEmptyReposServer();
    }

    @Test
    public void noop_when_no_changes_made() throws GitAPIException {
        //given a clone of an empty repo
        GitProcessor processor=new GitProcessor("gituser", "gitsecret", GIT_URL);
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        Iterable<RevCommit> initialCommits = processor.getGit().log().call();

        //when no changes are made
        processor.commitPushRepo(new Context());


        //then a new clone does not contain new commits
        processor.cloneRepo(ctx);
        Iterable<RevCommit> resultingCommits = processor.getGit().log().call();

        assertThat(countIterables(resultingCommits)).isEqualTo(countIterables(initialCommits));
    }

    public int countIterables(Iterable<RevCommit> resultingCommits) {
        int size = 0;
        for (RevCommit resultingCommit : resultingCommits) {
            size++;
        }
        return size;
    }

    @Test
	public void adds_and_deletes_files() throws GitAPIException, IOException {
        //given a clone of an empty repo
		GitProcessor processor=new GitProcessor("gituser", "gitsecret", GIT_URL);
        Context ctx = new Context();
        processor.cloneRepo(ctx);

        //when adding files
        //and asking to commit and push
        Path workDir = (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
        try(FileWriter writer = new FileWriter(workDir.resolve("afile.txt").toFile())){
            writer.append("hello.txt");
        }
        processor.commitPushRepo(new Context());

        //then file should be persisted
        Path secondClone = cloneRepo(processor);
        File secondCloneFile = secondClone.resolve("afile.txt").toFile();
        assertThat(secondCloneFile).exists();

		//when deleting file
        assertThat(secondCloneFile.delete()).isTrue();
        //and committing
        processor.commitPushRepo(new Context());

        //then file should be removed from repo
		Path thirdClone = cloneRepo(processor);
		File thirdCloneFile = thirdClone.resolve("afile.txt").toFile();
		assertThat(thirdCloneFile).doesNotExist();
	}

    public Path cloneRepo(GitProcessor processor) {
        Context ctx=new Context();
        processor.preCreate(ctx);

        return (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
    }


}
