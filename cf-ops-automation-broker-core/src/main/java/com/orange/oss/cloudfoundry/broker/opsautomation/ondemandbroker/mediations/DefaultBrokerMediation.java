package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBrokerMediation implements BrokerMediation {

	private static Logger logger=LoggerFactory.getLogger(DefaultBrokerMediation.class.getName());

	@Override
	public void preCreate() {
		logger.debug("noop default preCreate");
		
	}

	@Override
	public void postCreate() {
		logger.debug("noop default postCreate");
		
	}

	@Override
	public void preBind() {
		logger.debug("noop default preBind");
		
	}

	@Override
	public void postBind() {
		logger.debug("noop default postBind");
		
	}

	@Override
	public void preDelete() {
		logger.debug("noop default preDelete");		
	}

	@Override
	public void postDelete() {
		logger.debug("noop default postDelete");
		
	}

	@Override
	public void preUnBind() {
		logger.debug("noop default preUnbind");
		
	}

	@Override
	public void postUnBind() {
		logger.debug("noop default postUnbind");
		
	}
}
