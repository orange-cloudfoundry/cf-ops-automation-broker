package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;

public interface OsbProxy {

    GetLastServiceOperationResponse delegateProvision(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response);

    GetLastServiceOperationResponse delegateDeprovision(GetLastServiceOperationRequest pollingRequest, DeleteServiceInstanceRequest request, GetLastServiceOperationResponse response);

    CreateServiceInstanceBindingResponse delegateBind(CreateServiceInstanceBindingRequest request);

    void delegateUnbind(DeleteServiceInstanceBindingRequest request);
}
