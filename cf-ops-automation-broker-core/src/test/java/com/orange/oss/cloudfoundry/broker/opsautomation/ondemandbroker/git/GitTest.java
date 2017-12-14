package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
//TODO: consider moving into a springbootapplication instead, possibly within a static inner class of this test class
//is this test really meaningfully anyway ?
@EnableConfigurationProperties({GitProperties.class})
public class GitTest {

	@Autowired
	GitProperties gitProperties;

	@Test
	public void testGitProcessor() {

		GitProcessor processor=new GitProcessor(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), "committerName", "committerEmail", null, "master");
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(processor);
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());

		Context ctx=new Context();
		chain.create(ctx);

	}

	GitServer gitServer;

	@Before
	public void startGitServer() throws IOException, GitAPIException {
		gitServer = new GitServer();
		gitServer.startEphemeralReposServer(GitServer.NO_OP_INITIALIZER);
	}

	@After
	public void cleanUpGit() throws Exception {
		gitServer.stopAndCleanupReposServer();
	}

}
