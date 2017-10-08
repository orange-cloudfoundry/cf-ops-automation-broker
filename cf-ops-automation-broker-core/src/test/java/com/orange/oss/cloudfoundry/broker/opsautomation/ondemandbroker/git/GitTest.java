package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.BrokerMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediationSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.MediationChain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource("classpath:git.properties")
public class GitTest {

	@Autowired
	GitTestProperties gitProperties;
	
	@Test
	public void testGitMediation() {
		
		GitMediation mediation=new GitMediation(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl());
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(mediation);
		MediationChain chain=new MediationChain(mediations, new DefaultBrokerMediationSink());
		
		chain.create();
	}
	
}
