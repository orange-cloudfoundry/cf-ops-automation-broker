package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * default no-op implementation for broker mediatio
 * @author root
 *
 */
public class DefaultBrokerProcessor implements BrokerProcessor {

	private static final Logger logger=LoggerFactory.getLogger(DefaultBrokerProcessor.class.getName());

	@Override
	public void preCreate(Context ctx) {
		logger.debug("noop default preCreate");
		
	}

	@Override
	public void preGetLastCreateOperation(Context ctx) {
		logger.debug("noop default preGetLastCreateOperation");
	}

	@Override
	public void postGetLastCreateOperation(Context ctx) {
		logger.debug("noop default postGetLastCreateOperation");
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
