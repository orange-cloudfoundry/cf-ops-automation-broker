package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitTestProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.BrokerMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediationSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.MediationChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.GitPipelineTemplatingMediation;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class GlobalBrokerMediationChainTest {

	@Autowired
	GitTestProperties gitProperties;
	
	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;
	

	@Test
	public void testCompositeMediationChain() {
		
		GitMediation mediation=new GitMediation(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl());
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(mediation);
		//TODO: add credhub password generation
		mediations.add(new GitPipelineTemplatingMediation("on-demand-depl",this.manifestResource));		
		MediationChain chain=new MediationChain(mediations, new DefaultBrokerMediationSink());
		//FIXME: use bosh api call sink
		
		chain.create();
	}

	
}
