package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.AsyncServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public interface OsbProxy<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> {

    P delegate(Q request, P response);
}
