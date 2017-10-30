package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
@Ignore
public class GitTest {

	@Autowired
	GitTestProperties gitProperties;
	
	@Test
	public void testGitProcessor() {
		
		GitProcessor processor=new GitProcessor(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl());
		List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
		processors.add(processor);
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());
		
		chain.create();
	}
	
}
