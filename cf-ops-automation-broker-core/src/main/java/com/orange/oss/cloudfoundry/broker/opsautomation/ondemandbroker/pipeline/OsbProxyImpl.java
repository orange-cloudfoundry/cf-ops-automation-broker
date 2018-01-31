package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.*;

public class OsbProxyImpl<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> implements OsbProxy<Q> {
    private final String osbDelegateUser;
    private final String osbDelegatePassword;

    public OsbProxyImpl(String osbDelegateUser, String osbDelegatePassword) {
        this.osbDelegateUser = osbDelegateUser;
        this.osbDelegatePassword = osbDelegatePassword;
    }

    @Override
    public GetLastServiceOperationResponse delegate(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        return response;
    }
}
