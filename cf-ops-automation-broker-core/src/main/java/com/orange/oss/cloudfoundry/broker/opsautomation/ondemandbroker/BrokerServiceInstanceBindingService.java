package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

@Service
public class BrokerServiceInstanceBindingService implements ServiceInstanceBindingService  {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrokerServiceInstanceBindingService.class.getName());



	@Override
	public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest arg0) {
		return new CreateServiceInstanceBindingResponse();
	}

	@Override
	public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest arg0) {
	}

}
