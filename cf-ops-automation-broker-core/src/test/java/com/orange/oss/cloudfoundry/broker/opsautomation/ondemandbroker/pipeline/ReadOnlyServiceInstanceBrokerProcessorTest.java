package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerUnavailableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadOnlyServiceInstanceBrokerProcessorTest {

	ReadOnlyServiceInstanceBrokerProcessor processor;

	@Nested
	class whenReadOnlyModeSet {

		@BeforeEach
		void setUp() {
			processor= new ReadOnlyServiceInstanceBrokerProcessor(true, "user-facing-message");
		}

		@Test
		void rejects_service_instance_operations() {
			assertThrows503WithUserFacingMessage(() -> processor.preCreate(new Context()));
			assertThrows503WithUserFacingMessage(() -> processor.preUpdate(new Context()));
			assertThrows503WithUserFacingMessage(() -> processor.preDelete(new Context()));
			assertThrows503WithUserFacingMessage(() -> processor.preGetInstance(new Context()));
			assertThrows503WithUserFacingMessage(() -> processor.preGetLastOperation(new Context()));
		}

		@Test
		void ignores_service_binding_operations() {
			processor.preBind(new Context());
			processor.postBind(new Context());
			processor.preUnBind(new Context());
			processor.postUnBind(new Context());
		}

		private AbstractThrowableAssert<?, ? extends Throwable> assertThrows503WithUserFacingMessage(
			ThrowableAssert.ThrowingCallable serviceInstanceOperation) {
			return assertThatThrownBy(serviceInstanceOperation).isInstanceOf(
				ServiceBrokerUnavailableException.class)
				.hasMessageContaining("user-facing-message");
		}

	}

	@Nested
	class whenReadOnlyModeNotSet {

		@BeforeEach
		void setUp() {
			processor= new ReadOnlyServiceInstanceBrokerProcessor(false, "user-facing-message");
		}

		@Test
		void ignores_service_instance_operations() {
			processor.preCreate(new Context());
			processor.preUpdate(new Context());
			processor.preDelete(new Context());
			processor.preGetInstance(new Context());
			processor.preGetLastOperation(new Context());
		}

		@Test
		void ignores_service_binding_operations() {
			processor.preBind(new Context());
			processor.postBind(new Context());
			processor.preUnBind(new Context());
			processor.postUnBind(new Context());
		}

	}

}