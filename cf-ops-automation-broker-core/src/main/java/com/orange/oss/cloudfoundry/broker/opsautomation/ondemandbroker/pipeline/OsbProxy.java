package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.*;

public interface OsbProxy {

    GetLastServiceOperationResponse delegateProvision(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response);

    GetLastServiceOperationResponse delegateDeprovision(GetLastServiceOperationRequest pollingRequest, DeleteServiceInstanceRequest request, GetLastServiceOperationResponse response);

    CreateServiceInstanceBindingResponse delegateBind(CreateServiceInstanceBindingRequest request);

    void delegateUnbind(DeleteServiceInstanceBindingRequest request);
}
