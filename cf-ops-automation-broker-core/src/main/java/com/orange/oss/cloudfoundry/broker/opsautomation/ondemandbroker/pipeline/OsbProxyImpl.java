package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.AsyncServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public class OsbProxyImpl<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> implements OsbProxy<Q, P> {
    @Override
    public P delegate(Q request, P response) {
        return response;
    }
}
