package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitTestProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
public class GitPipelineTemplatingProcessorIT {

	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;

	
	@Autowired
	GitTestProperties gitProperties;
	
	@Test
	public void testTemplatingProcessor() {
		
		GitProcessor processor=new GitProcessor(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl(), "committerName", "committerEmail");
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(processor);
		processors.add(new GitPipelineTemplatingProcessor("on-demand-depl",this.manifestResource));
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());

		Context ctx=new Context();
		chain.create(ctx);

	}

}
