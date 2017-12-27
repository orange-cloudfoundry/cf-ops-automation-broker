package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink of broker processors
 * Terminal point, typically output to a foreign broker or a backend
 *
 * @author poblin-orange
 */
public class DefaultBrokerSink implements BrokerSink {
    private static Logger logger = LoggerFactory.getLogger(DefaultBrokerProcessor.class.getName());

    @Override
    public void create(Context ctx) {
        logger.debug("noop default Create");
    }

    @Override
    public void getLastOperation(Context ctx) {
        logger.debug("noop default getLastCreateOperation");
    }


    @Override
    public void bind(Context ctx) {
        logger.debug("noop default Bind");
    }


    @Override
    public void delete(Context ctx) {
        logger.debug("noop default delete");
    }


    @Override
    public void unBind(Context ctx) {
        logger.debug("noop default unbind");
    }

    @Override
    public void update(Context ctx) {
        logger.debug("noop default update");
    }

}
