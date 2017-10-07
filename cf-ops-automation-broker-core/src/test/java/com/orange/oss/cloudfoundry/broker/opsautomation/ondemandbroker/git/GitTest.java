package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.BrokerMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediationSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.MediationChain;

//@RunWith(SpringRunner.class)
@PropertySource("classpath:/git.properties")
@SpringBootTest
public class GitTest {

	
	@Value("${gitUser}")
	private String gitUser;
	@Value("${gitPassword}")
	private String gitPassword;
	@Value("${gitBaseUrl}")	
	private String gitUrl;
	
	
	@Test
	public void testGitMediation() {
		
		GitMediation mediation=new GitMediation(gitUser, gitPassword, gitUrl);
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(mediation);
		MediationChain chain=new MediationChain(mediations, new DefaultBrokerMediationSink());
		
		chain.create();
		
		
		
		
	}
	
}
