package com.orange.oss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
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
		CreateServiceInstanceRequest req = CreateServiceInstanceRequest.builder()
				.serviceInstanceId(serviceDefId)
				.planId(planId)
				//omitted context
				.build();

		CreateServiceInstanceResponse si = this.service.createServiceInstance(req);

		String appGuid="appGuid";
		Map<String, Object> bindResource = new HashMap<>();
		CreateServiceInstanceBindingRequest breq = CreateServiceInstanceBindingRequest.builder()
				.serviceDefinitionId(serviceDefId)
				.planId(planId)
				.bindResource(BindResource.builder()
						.appGuid("app_guid")
						.build())
				.build();

		CreateServiceInstanceBindingResponse sib = this.binding.createServiceInstanceBinding(breq);

	}

}

@SpringBootApplication
class OpsAutomationServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpsAutomationServiceBrokerApplication.class, args);
	}

	@Bean
	@ConditionalOnMissingBean
	public ProcessorChain processorChain() {
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(new DefaultBrokerProcessor());
		DefaultBrokerSink sink=new DefaultBrokerSink();
		return new ProcessorChain(processors, sink);
	}


}
