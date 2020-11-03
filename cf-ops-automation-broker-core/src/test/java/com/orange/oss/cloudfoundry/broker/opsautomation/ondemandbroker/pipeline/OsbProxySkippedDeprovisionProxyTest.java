package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Note: we're testing the proxy pattern applied here. Might be overkill, falling in the "too simple to test" category.
 * See https://stackoverflow.com/questions/18714489/whats-the-best-way-to-unit-test-classes-that-delegate-the-work-to-others/18714573
 *
 * If you find yourself struggling maintaining this test, feel free to remove the delagation test cases
 * and only keep the non-delegation test case.
 */
@ExtendWith(MockitoExtension.class)
class OsbProxySkippedDeprovisionProxyTest {

	@Mock
	OsbProxy subject;
	OsbProxySkippedDeprovisionProxy osbProxySkippedDeprovisionProxy;

	@BeforeEach
	void setUp() {
		osbProxySkippedDeprovisionProxy = new OsbProxySkippedDeprovisionProxy(subject);
	}

	@Test
	void delegateProvision() {
		//given
		GetLastServiceOperationResponse subjectResponse =
			mock(GetLastServiceOperationResponse.class);
		when(subject.delegateProvision(any(), any(), any())).thenReturn(subjectResponse);

		GetLastServiceOperationRequest getLastServiceOperationRequest = mock(GetLastServiceOperationRequest.class);
		CreateServiceInstanceRequest createServiceInstanceRequest = mock(CreateServiceInstanceRequest.class);
		GetLastServiceOperationResponse getLastServiceOperationResponse = mock(GetLastServiceOperationResponse.class);

		//when
		GetLastServiceOperationResponse response = osbProxySkippedDeprovisionProxy
			.delegateProvision(
				getLastServiceOperationRequest,
				createServiceInstanceRequest,
				getLastServiceOperationResponse);

		//then
		assertThat(response).isSameAs(subjectResponse);
		verify(subject).delegateProvision(getLastServiceOperationRequest, createServiceInstanceRequest,
			getLastServiceOperationResponse);
	}

	@Test
	void doesNotDelegateDeprovision() {
		//given
		GetLastServiceOperationRequest getLastServiceOperationRequest = mock(GetLastServiceOperationRequest.class);
		DeleteServiceInstanceRequest deleteServiceInstanceRequest = mock(DeleteServiceInstanceRequest.class);
		GetLastServiceOperationResponse getLastServiceOperationResponse = mock(GetLastServiceOperationResponse.class);

		//when
		GetLastServiceOperationResponse response = osbProxySkippedDeprovisionProxy
			.delegateDeprovision(
				getLastServiceOperationRequest,
				deleteServiceInstanceRequest,
				getLastServiceOperationResponse);

		//then
		verify(subject, never()).delegateDeprovision(getLastServiceOperationRequest, deleteServiceInstanceRequest,
			getLastServiceOperationResponse);
		assertThat(response).isEqualTo(GetLastServiceOperationResponse.builder().deleteOperation(true).operationState(
			OperationState.SUCCEEDED).build());
	}

	@Test
	void delegateBind() {
		//given
		CreateServiceInstanceBindingResponse subjectResponse =
			mock(CreateServiceInstanceBindingResponse.class);
		when(subject.delegateBind(any())).thenReturn(subjectResponse);

		CreateServiceInstanceBindingRequest request = mock(CreateServiceInstanceBindingRequest.class);

		//when
		CreateServiceInstanceBindingResponse response = osbProxySkippedDeprovisionProxy
			.delegateBind(request);

		//then
		assertThat(response).isSameAs(subjectResponse);
		verify(subject).delegateBind(request);
	}

	@Test
	void delegateUnbind() {
		//given
		doNothing().when(subject).delegateUnbind(any());

		DeleteServiceInstanceBindingRequest request =
			mock(DeleteServiceInstanceBindingRequest.class);

		//when
		osbProxySkippedDeprovisionProxy.delegateUnbind(request);

		//then
		verify(subject).delegateUnbind(request);
	}

}