package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerUnavailableException;
import org.springframework.util.Assert;

public class ReadOnlyServiceInstanceBrokerProcessor extends DefaultBrokerProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ReadOnlyServiceInstanceBrokerProcessor.class.getName());

	private boolean serviceInstanceReadOnlyMode;

	private String serviceInstanceReadOnlyMessage;

	public ReadOnlyServiceInstanceBrokerProcessor(boolean serviceInstanceReadOnlyMode,
		String serviceInstanceReadOnlyMessage) {
		//See https://www.baeldung.com/spring-assert#1-notnull
		Assert.notNull(serviceInstanceReadOnlyMessage, "expected non null serviceInstanceReadOnlyMessage");
		this.serviceInstanceReadOnlyMode = serviceInstanceReadOnlyMode;
		this.serviceInstanceReadOnlyMessage = serviceInstanceReadOnlyMessage;
		logger.info("serviceInstanceReadOnlyMode={} with message={}", serviceInstanceReadOnlyMode,
			serviceInstanceReadOnlyMessage);
	}

	@Override
	public void preCreate(Context ctx) {
		throwExceptionIfReadOnlyModeEnabled();
	}

	@Override
	public void preGetLastOperation(Context ctx) {
		throwExceptionIfReadOnlyModeEnabled();
	}

	@Override
	public void preDelete(Context ctx) {
		throwExceptionIfReadOnlyModeEnabled();
	}

	@Override
	public void preUpdate(Context ctx) {
		throwExceptionIfReadOnlyModeEnabled();
	}

	@Override
	public void preGetInstance(Context ctx) {
		throwExceptionIfReadOnlyModeEnabled();
	}

	private void throwExceptionIfReadOnlyModeEnabled() {
		if (serviceInstanceReadOnlyMode) {
			logger.info("Read-only mode is turned on, rejecting incoming request");
			throw new ServiceBrokerUnavailableException(serviceInstanceReadOnlyMessage);
		}
	}

	//
	// Supports integration tests that mutuate the readOnly mode
	//

	public boolean isServiceInstanceReadOnlyMode() {
		return serviceInstanceReadOnlyMode;
	}

	public void setServiceInstanceReadOnlyMode(boolean serviceInstanceReadOnlyMode) {
		this.serviceInstanceReadOnlyMode = serviceInstanceReadOnlyMode;
	}

	public String getServiceInstanceReadOnlyMessage() {
		return serviceInstanceReadOnlyMessage;
	}

	public void setServiceInstanceReadOnlyMessage(String serviceInstanceReadOnlyMessage) {
		this.serviceInstanceReadOnlyMessage = serviceInstanceReadOnlyMessage;
	}

}
