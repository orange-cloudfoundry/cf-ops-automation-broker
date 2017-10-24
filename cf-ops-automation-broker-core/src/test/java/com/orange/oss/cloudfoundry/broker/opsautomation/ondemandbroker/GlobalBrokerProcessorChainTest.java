package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.GitPipelineTemplatingProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitTestProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class GlobalBrokerProcessorChainTest {

	@Autowired
	GitTestProperties gitProperties;
	
	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;
	

	@Test
	public void testCompositeProcessorChain() {
		
		GitProcessor processor=new GitProcessor(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl());
		List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
		processors.add(processor);
		//TODO: add credhub password generation
		processors.add(new GitPipelineTemplatingProcessor("on-demand-depl",this.manifestResource));
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());
		//FIXME: use bosh api call sink
		
		chain.create();
	}

	
}
