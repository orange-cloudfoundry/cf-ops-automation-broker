package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public interface OsbProxy<Q extends ServiceBrokerRequest> {

    GetLastServiceOperationResponse delegate(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response);
}
