package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ProcessorChainTest {

	private static Logger logger=LoggerFactory.getLogger(ProcessorChainTest.class.getName());

	private final ArrayList<String> invocations = new ArrayList<>();

	private BrokerProcessor processor1 = new BrokerProcessor() {

		@Override
		public void preCreate(Context ctx) {
			recordInvocation("preCreate 1");
		}

		@Override
		public void preBind(Context ctx) {
			recordInvocation("preBind 1");
		}

		@Override
		public void preGetLastOperation(Context ctx) {
			recordInvocation("preGetLastCreateOperation 1");
		}

		@Override
		public void postCreate(Context ctx) {
			recordInvocation("post Create 1");
		}

		@Override
		public void postGetLastOperation(Context ctx) {
			recordInvocation("preGetLastCreateOperation 1");
		}

		@Override
		public void postBind(Context ctx) {
			recordInvocation("post Bind 1");
		}

		@Override
		public void cleanUp(Context ctx) {
			recordInvocation("cleanUp 1");
		}

		@Override
		public void preDelete(Context ctx) {
			recordInvocation("pre delete 1");

		}

		@Override
		public void postDelete(Context ctx) {
			recordInvocation("post delete 1");

		}

		@Override
		public void preUnBind(Context ctx) {
			recordInvocation("pre unbind 1");

		}

		@Override
		public void postUnBind(Context ctx) {
			recordInvocation("post unbind 1");

		}

		@Override
		public void preUpdate(Context ctx) {
			recordInvocation("pre update 1");

		}

		@Override
		public void postUpdate(Context ctx) {
			recordInvocation("post update 1");

		}

		@Override
		public void preGetInstance(Context ctx) {
			recordInvocation("pre getinstance 1");
		}

		@Override
		public void postGetInstance(Context ctx) {
			recordInvocation("post getinstance 1");
		}
	};
	private BrokerProcessor processor2 = new BrokerProcessor() {

		@Override
		public void preCreate(Context ctx) {
			recordInvocation("preCreate 2");
		}

		@Override
		public void preGetLastOperation(Context ctx) {
			recordInvocation("preGetLastCreateOperation 2");
		}

		@Override
		public void preBind(Context ctx) {
			recordInvocation("preBind 2");
		}


		@Override
		public void postCreate(Context ctx) {
			recordInvocation("post Create 2");
		}

		@Override
		public void postGetLastOperation(Context ctx) {
			recordInvocation("preGetLastCreateOperation 2");
		}

		@Override
		public void postBind(Context ctx) {
			recordInvocation("post Bind 2");
		}

		@Override
		public void preGetInstance(Context ctx) {
			recordInvocation("pre getinstance 2");
		}

		@Override
		public void postGetInstance(Context ctx) {
			recordInvocation("post getinstance 2");
		}

		@Override
		public void preDelete(Context ctx) {
			recordInvocation("pre delete 2");

		}

		@Override
		public void postDelete(Context ctx) {
			recordInvocation("post delete 2");

		}

		@Override
		public void preUnBind(Context ctx) {
			recordInvocation("pre unbind 2");

		}

		@Override
		public void postUnBind(Context ctx) {
			recordInvocation("post unbind 2");

		}

		@Override
		public void preUpdate(Context ctx) {
			recordInvocation("pre update 2");

		}

		@Override
		public void postUpdate(Context ctx) {
			recordInvocation("post update 2");

		}

		@Override
		public void cleanUp(Context ctx) {
			recordInvocation("cleanUp 2");
		}

	};

	@Test
	public void testDefaultProcessorsOrder() {
		//given default processors are added
		List<BrokerProcessor> defaultProcessors= new ArrayList<>();
		defaultProcessors.add(processor1);
		defaultProcessors.add(processor2);

		DefaultBrokerSink sink=new DefaultBrokerSink();
		ProcessorChain chain=new ProcessorChain(defaultProcessors, null, sink);


		//then default order is respected
		chain.create(new Context());

		List<String> expectedCreateInvocations = Arrays.asList(
			"preCreate 1",
			"preCreate 2",
			"post Create 2",
			"post Create 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedCreateInvocations);
		invocations.clear();

		chain.getLastOperation(new Context());
		List<String> expectedLastOperationsInvocations = Arrays.asList(
			"preGetLastCreateOperation 1",
			"preGetLastCreateOperation 2",
			"preGetLastCreateOperation 2",
			"preGetLastCreateOperation 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedLastOperationsInvocations);
		invocations.clear();


		chain.bind(new Context());
		List<String> expectedBindInvocations = Arrays.asList(
			"preBind 1",
			"preBind 2",
			"post Bind 2",
			"post Bind 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedBindInvocations);
		invocations.clear();

		chain.unBind(new Context());
		List<String> expectedUnbindInvocations = Arrays.asList(
			"pre unbind 1",
			"pre unbind 2",
			"post unbind 2",
			"post unbind 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedUnbindInvocations);
		invocations.clear();

		chain.update(new Context());
		List<String> expectedUpdateInvocations = Arrays.asList(
			"pre update 1",
			"pre update 2",
			"post update 2",
			"post update 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedUpdateInvocations);
		invocations.clear();

		chain.getInstance(new Context());
		List<String> expectedGetInstanceInvocations = Arrays.asList(
			"pre getinstance 1",
			"pre getinstance 2",
			"post getinstance 2",
			"post getinstance 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedGetInstanceInvocations);
		invocations.clear();

		chain.delete(new Context());
		List<String> expectedDeleteInvocations = Arrays.asList(
			"pre delete 1",
			"pre delete 2",
			"post delete 2",
			"post delete 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedDeleteInvocations);
		invocations.clear();

	}

	@Test
	public void testDeleteSpecificProcessorsOrder() {
		//given default processors are added
		List<BrokerProcessor> defaultProcessors= new ArrayList<>();
		defaultProcessors.add(processor1);
		defaultProcessors.add(processor2);

		//given delete processors are added using a different order
		List<BrokerProcessor> deleteProcessors= new ArrayList<>();
		deleteProcessors.add(processor2);
		deleteProcessors.add(processor1);

		DefaultBrokerSink sink=new DefaultBrokerSink();
		ProcessorChain chain=new ProcessorChain(defaultProcessors, deleteProcessors, sink);




		//then the delete specific processors are invoked
		chain.delete(new Context());
		List<String> expectedDeleteInvocations = Arrays.asList(
			"pre delete 2",
			"pre delete 1",
			"post delete 1",
			"post delete 2",
			"cleanUp 1",
			"cleanUp 2");
		assertThat(invocations).isEqualTo(expectedDeleteInvocations);
		invocations.clear();

		//and the default order is respected for other steps than delete
		chain.create(new Context());
		List<String> expectedCreateInvocations = Arrays.asList(
			"preCreate 1",
			"preCreate 2",
			"post Create 2",
			"post Create 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedCreateInvocations);
		invocations.clear();

		chain.getLastOperation(new Context());
		List<String> expectedLastOperationsInvocations = Arrays.asList(
			"preGetLastCreateOperation 1",
			"preGetLastCreateOperation 2",
			"preGetLastCreateOperation 2",
			"preGetLastCreateOperation 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedLastOperationsInvocations);
		invocations.clear();


		chain.bind(new Context());
		List<String> expectedBindInvocations = Arrays.asList(
			"preBind 1",
			"preBind 2",
			"post Bind 2",
			"post Bind 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedBindInvocations);
		invocations.clear();

		chain.unBind(new Context());
		List<String> expectedUnbindInvocations = Arrays.asList(
			"pre unbind 1",
			"pre unbind 2",
			"post unbind 2",
			"post unbind 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedUnbindInvocations);
		invocations.clear();

		chain.update(new Context());
		List<String> expectedUpdateInvocations = Arrays.asList(
			"pre update 1",
			"pre update 2",
			"post update 2",
			"post update 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedUpdateInvocations);
		invocations.clear();

		chain.getInstance(new Context());
		List<String> expectedGetInstanceInvocations = Arrays.asList(
			"pre getinstance 1",
			"pre getinstance 2",
			"post getinstance 2",
			"post getinstance 1",
			"cleanUp 2",
			"cleanUp 1");
		assertThat(invocations).isEqualTo(expectedGetInstanceInvocations);
		invocations.clear();

	}

	private void recordInvocation(String processorStep) {
		logger.info(processorStep);
		invocations.add(processorStep);
	}

}
