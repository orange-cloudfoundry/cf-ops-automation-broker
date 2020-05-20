package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PasswordGenProcessorIT {

	

	@Test
	public void testPasswordGenProcessor() {

		String url="https://credhub.internal.paas";
		String instanceGroupName="ig";
		String propertyName="prop";
		

		PasswordGenProcessor processor=new PasswordGenProcessor(url,instanceGroupName, propertyName);
		
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(processor);
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());
		Context ctx=new Context();
		chain.create(ctx);

	}

	
}
