package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.GitPipelineTemplatingProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
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

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
@EnableConfigurationProperties({GitProperties.class})
public class GlobalBrokerProcessorChainIT {

	@Autowired
    GitProperties gitProperties;
	
	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;
	

	@Test
	public void testCompositeProcessorChain() {
		
		GitProcessor processor=new GitProcessor(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), gitProperties.committerName(), gitProperties.committerEmail(), null);
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(processor);
		//TODO: add credhub password generation
		processors.add(new GitPipelineTemplatingProcessor("on-demand-depl",this.manifestResource));
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());
		//FIXME: use bosh api call sink

		Context ctx=new Context();
		chain.create(ctx);

	}

	GitServer gitServer;

	@Before
	public void startGitServer() throws IOException, GitAPIException {
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
