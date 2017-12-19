package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This tests may work standalone, simulating a git server by embedding one.
 * It may otherwise also be run with a locally modified version of git.properties pointing to a private git server:
 * This supports asserting that clone of paas-template works well.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:git.properties")
@EnableConfigurationProperties({GitProperties.class})
public class GitIT {

	@SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
	GitProperties gitProperties;

	@Test
	public void testGitProcessor() {

		GitProcessor processor=new GitProcessor(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), "committerName", "committerEmail", null);
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(processor);
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());

		Context ctx=new Context();
		ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
		ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "feature-COAB-cassandra-IT");
		chain.create(ctx);

	}

	GitServer gitServer;

	@Before
	public void startGitServer() throws IOException, GitAPIException {
		gitServer = new GitServer();

		Consumer<Git> initPaasTemplate= git -> {
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
		};
		gitServer.startEphemeralReposServer(initPaasTemplate);
	}

	void createDir(Path dir) throws IOException {
		Files.createDirectories(dir);
		//TODO: create .gitignore
		try(Writer writer = new FileWriter(dir.resolve(".gitkeep").toFile())) {
			writer.write("Please keep me");
		}
	}

	@After
	public void cleanUpGit() throws Exception {
		gitServer.stopAndCleanupReposServer();
	}

}
