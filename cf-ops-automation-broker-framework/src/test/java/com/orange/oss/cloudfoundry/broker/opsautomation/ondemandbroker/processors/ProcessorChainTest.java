package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class ProcessorChainTest {

	private static Logger logger=LoggerFactory.getLogger(ProcessorChainTest.class.getName());

	@Test
	public void testInvocationChain() {
		List<BrokerProcessor> processors= new ArrayList<>();
		processors.add(new BrokerProcessor() {
			
			@Override
			public void preCreate(Context ctx) {
				logger.info("preCreate 1");
			}
			
			@Override
			public void preBind(Context ctx) {
				logger.info("preBind 1");
			}

			@Override
			public void preGetLastOperation(Context ctx) { logger.info("preGetLastCreateOperation 1"); }

			@Override
			public void postCreate(Context ctx) {
				logger.info("post Create 1");
			}

			@Override
			public void postGetLastOperation(Context ctx) { logger.info("preGetLastCreateOperation 1"); }

			@Override
			public void postBind(Context ctx) {
				logger.info("post Bind 1");
			}

			@Override
			public void cleanUp(Context ctx) {
				logger.info("cleanUp 1");
			}

			@Override
			public void preDelete(Context ctx) {
				logger.info("pre delete 1");
				
			}

			@Override
			public void postDelete(Context ctx) {
				logger.info("post delete 1");
				
			}

			@Override
			public void preUnBind(Context ctx) {
				logger.info("post unbind 1");
				
			}

			@Override
			public void postUnBind(Context ctx) {
				logger.info("post unbind 1");
				
			}

			@Override
			public void preUpdate(Context ctx) {
				logger.info("pre update 1");

			}

			@Override
			public void postUpdate(Context ctx) {
				logger.info("post update 1");

			}

			@Override
			public void preGetInstance(Context ctx) {
				logger.info("pre getinstance 1");
			}

			@Override
			public void postGetInstance(Context ctx) {
				logger.info("post getinstance 1");
			}
		});
		
		processors.add(new BrokerProcessor() {
			
			@Override
			public void preCreate(Context ctx) {
				logger.info("preCreate 2");
			}

			@Override
			public void preGetLastOperation(Context ctx) { logger.info("preGetLastCreateOperation 2"); }

			@Override
			public void preBind(Context ctx) {
				logger.info("preBind 2");
			}


			@Override
			public void postCreate(Context ctx) {
				logger.info("post Create 2");
			}

			@Override
			public void postGetLastOperation(Context ctx) { logger.info("preGetLastCreateOperation 2"); }

			@Override
			public void postBind(Context ctx) {
				logger.info("post Bind 2");
			}

			@Override
			public void preGetInstance(Context ctx) { logger.info("pre getinstance 2"); }

			@Override
			public void postGetInstance(Context ctx) { logger.info("post getinstance 2"); }
			@Override
			public void preDelete(Context ctx) {
				logger.info("pre delete 2");
				
			}

			@Override
			public void postDelete(Context ctx) {
				logger.info("post delete 2");
				
			}

			@Override
			public void preUnBind(Context ctx) {
				logger.info("pre unbind 2");
				
			}

			@Override
			public void postUnBind(Context ctx) {
				logger.info("post unbind 2");
				
			}

			@Override
			public void preUpdate(Context ctx) {
				logger.info("pre update 2");

			}

			@Override
			public void postUpdate(Context ctx) {
				logger.info("pre update 2");

			}

			@Override
			public void cleanUp(Context ctx) {
				logger.info("cleanUp 2");
			}

		});

		
		DefaultBrokerSink sink=new DefaultBrokerSink();
		ProcessorChain chain=new ProcessorChain(processors, sink);
		Context ctx=new Context();
		chain.create(ctx);

		Context ctx1 =new Context();
		chain.getLastOperation(ctx1);
		chain.bind(new Context());
		chain.unBind(new Context());
		chain.delete(new Context());
	
	
		
		
		
	}
	
}
