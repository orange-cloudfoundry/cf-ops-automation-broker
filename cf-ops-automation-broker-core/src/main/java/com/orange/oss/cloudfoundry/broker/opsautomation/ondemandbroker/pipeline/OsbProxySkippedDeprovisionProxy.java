package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * Support configuration into which service instances can be undeleted by operators. This requires nested brokers to
 * not unprovision service instances (e.g. in the case of cf-mysql-release this would result into the mysql schema be
 * deleted, preventing future undeletes). This proxy is conditionally fronting the OsbProxyImpl depending on
 * requested configuration.
 *
 * Applies the Proxy pattern https://en.wikipedia.org/wiki/Proxy_pattern
 */
public class OsbProxySkippedDeprovisionProxy
	implements OsbProxy {

	private static Logger logger = LoggerFactory.getLogger(OsbProxySkippedDeprovisionProxy.class.getName());
	private final OsbProxy subject;

	public OsbProxySkippedDeprovisionProxy(OsbProxy subject) {
		this.subject = subject;
	}

	@Override
	public GetLastServiceOperationResponse delegateProvision(GetLastServiceOperationRequest pollingRequest,
		CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
		return subject.delegateProvision(pollingRequest, request, response);
	}

	@Override
	public GetLastServiceOperationResponse delegateDeprovision(GetLastServiceOperationRequest pollingRequest,
		DeleteServiceInstanceRequest request, GetLastServiceOperationResponse response) {
		logger.info("Skipping delegation of deprovision request as instructed to");
		//noinspection UnnecessaryLocalVariable
		GetLastServiceOperationResponse deprovisionSuccessResponse = GetLastServiceOperationResponse.builder().deleteOperation(true)
			.operationState(OperationState.SUCCEEDED).build();
		return deprovisionSuccessResponse;
	}

	@Override
	public CreateServiceInstanceBindingResponse delegateBind(CreateServiceInstanceBindingRequest request) {
		return subject.delegateBind(request);
	}

	@Override
	public void delegateUnbind(DeleteServiceInstanceBindingRequest request) {
		subject.delegateUnbind(request);
	}

}
