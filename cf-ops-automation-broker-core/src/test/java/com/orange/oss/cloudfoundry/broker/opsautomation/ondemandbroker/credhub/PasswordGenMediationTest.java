package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.BrokerMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediationSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.MediationChain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest

public class PasswordGenMediationTest {

	

	@Test
	public void testPasswordGenMediation() {

		String url="https://credhub.internal.paas";
		String instanceGroupName="ig";
		String propertyName="prop";
		

		PasswordGenMediation mediation=new PasswordGenMediation(url,instanceGroupName, propertyName);
		
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(mediation);
		MediationChain chain=new MediationChain(mediations, new DefaultBrokerMediationSink());
		chain.create();
	}

	
}
