package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink of broker mediation
 * Terminal point, typically output to a foreign broker or a backend
 * @author poblin-orange
 *
 */
public class DefaultBrokerMediationSink  implements BrokerMediationSink {
	private static Logger logger=LoggerFactory.getLogger(DefaultBrokerMediation.class.getName());

	@Override
	public void create() {
		logger.debug("noop default Create");
		
	}


	@Override
	public void bind() {
		logger.debug("noop default Bind");
		
	}


	@Override
	public void delete() {
		logger.debug("noop default delete");
		
	}


	@Override
	public void unBind() {
		logger.debug("noop default unbind");		
	}

}
