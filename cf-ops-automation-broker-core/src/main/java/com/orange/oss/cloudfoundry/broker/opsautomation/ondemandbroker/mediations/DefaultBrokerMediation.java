package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * default no-op implementation for broker mediatio
 * @author root
 *
 */
public class DefaultBrokerMediation implements BrokerMediation {

	private static Logger logger=LoggerFactory.getLogger(DefaultBrokerMediation.class.getName());

	@Override
	public void preCreate(Context ctx) {
		logger.debug("noop default preCreate");
		
	}

	@Override
	public void postCreate(Context ctx) {
		logger.debug("noop default postCreate");
		
	}

	@Override
	public void preBind(Context ctx) {
		logger.debug("noop default preBind");
		
	}

	@Override
	public void postBind(Context ctx) {
		logger.debug("noop default postBind");
		
	}

	@Override
	public void preDelete(Context ctx) {
		logger.debug("noop default preDelete");		
	}

	@Override
	public void postDelete(Context ctx) {
		logger.debug("noop default postDelete");
		
	}

	@Override
	public void preUnBind(Context ctx) {
		logger.debug("noop default preUnbind");
		
	}

	@Override
	public void postUnBind(Context ctx) {
		logger.debug("noop default postUnbind");
		
	}
}
