package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class MediationChainTest {

	private static Logger logger=LoggerFactory.getLogger(MediationChainTest.class.getName());

	@Test
	public void testInvocationChain() {
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(new BrokerMediation() {
			
			@Override
			public void preCreate() {
				logger.info("preCreate 1");
			}
			
			@Override
			public void preBind() {
				logger.info("preBind 1");
			}
			
			@Override
			public void postCreate() {
				logger.info("post Create 1");
			}
			
			@Override
			public void postBind() {
				logger.info("post Bind 1");
			}

			@Override
			public void preDelete() {
				logger.info("pre delete 1");
				
			}

			@Override
			public void postDelete() {
				logger.info("post delete 1");
				
			}

			@Override
			public void preUnBind() {
				logger.info("post unbind 1");
				
			}

			@Override
			public void postUnBind() {
				logger.info("post unbind 1");
				
			}
		});
		
		mediations.add(new BrokerMediation() {
			
			@Override
			public void preCreate() {
				logger.info("preCreate 2");
			}
			
			@Override
			public void preBind() {
				logger.info("preBind 2");
			}
			
			@Override
			public void postCreate() {
				logger.info("post Create 2");
			}
			
			@Override
			public void postBind() {
				logger.info("post Bind 2");
			}

			@Override
			public void preDelete() {
				logger.info("pre delete 2");
				
			}

			@Override
			public void postDelete() {
				logger.info("post delete 2");
				
			}

			@Override
			public void preUnBind() {
				logger.info("pre unbind 2");
				
			}

			@Override
			public void postUnBind() {
				logger.info("post unbind 2");
				
			}
		});

		
		DefaultBrokerMediationSink sink=new DefaultBrokerMediationSink();
		MediationChain chain=new MediationChain(mediations, sink);
		chain.create();
		chain.bind();
		chain.unBind();
		chain.delete();
	
	
		
		
		
	}
	
}
