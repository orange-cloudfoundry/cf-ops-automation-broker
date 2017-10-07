package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OpsAutomationServiceBrokerApplicationTests {

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
				orgId, spaceId);
		CreateServiceInstanceResponse si = this.service.createServiceInstance(req);

		String appGuid="appGuid";
		Map<String, Object> bindResource = new HashMap<String, Object>();
		CreateServiceInstanceBindingRequest breq = new CreateServiceInstanceBindingRequest(serviceDefId,planId,appGuid,bindResource);

		CreateServiceInstanceBindingResponse sib = this.binding.createServiceInstanceBinding(breq);

	}

}
