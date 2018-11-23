package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.SimpleGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
@EnableConfigurationProperties({GitProperties.class})
public class GitPipelineTemplatingProcessorIT {

	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;

	
	@Autowired
	GitProperties gitProperties;
	
	@Test
	public void testTemplatingProcessor() {
		
		SimpleGitManager processor=new SimpleGitManager(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), "committerName", "committerEmail", null);
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(new GitProcessor(processor, null));
		processors.add(new GitPipelineTemplatingProcessor("on-demand-depl",this.manifestResource));
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());

		Context ctx=new Context();
		chain.create(ctx);

	}

	private GitServer gitServer;

	@Before
	public void startGitServer() throws IOException {
		gitServer = new GitServer();
		gitServer.startEphemeralReposServer(GitServer.NO_OP_INITIALIZER);
		//FIXME: initialize git mock repo or reference publicly accessible git repo
		//containing expected hazelcast/template/hazelcast.yml
	}

	@After
	public void cleanUpGit() throws Exception {
		gitServer.stopAndCleanupReposServer();
	}


}
