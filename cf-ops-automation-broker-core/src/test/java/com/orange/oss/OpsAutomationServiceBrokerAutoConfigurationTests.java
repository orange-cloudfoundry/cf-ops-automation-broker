package com.orange.oss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OpsAutomationServiceBrokerAutoConfigurationTests {

	@Autowired
	Catalog catalog;

	@Autowired
	ServiceInstanceService service;

	@Autowired
	ServiceInstanceBindingService binding;

	@Test
	public void endToEndTest() {

		List<ServiceDefinition> defs = this.catalog.getServiceDefinitions();
		ServiceDefinition def = defs.get(0);
		
		String serviceDefId=def.getId();
		String planId=def.getPlans().get(0).getId();

		String spaceId = "spaceId";
		String orgId = "orgId";
		CreateServiceInstanceRequest req = new CreateServiceInstanceRequest(serviceDefId, planId,
				orgId, spaceId, new HashMap<>());
		CreateServiceInstanceResponse si = this.service.createServiceInstance(req);

		String appGuid="appGuid";
		Map<String, Object> bindResource = new HashMap<String, Object>();
		CreateServiceInstanceBindingRequest breq = new CreateServiceInstanceBindingRequest(serviceDefId,planId,appGuid,bindResource);

		CreateServiceInstanceBindingResponse sib = this.binding.createServiceInstanceBinding(breq);

	}

}

@SpringBootApplication
class OpsAutomationServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpsAutomationServiceBrokerApplication.class, args);
	}

	@Bean
	public ProcessorChain processorChain() {
		List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
		processors.add(new DefaultBrokerProcessor());
		DefaultBrokerSink sink=new DefaultBrokerSink();
		ProcessorChain chain=new ProcessorChain(processors, sink);
		return chain;
	}


}
