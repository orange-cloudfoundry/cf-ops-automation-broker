package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingRequest;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingResponse;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aCatalog;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aDeleteServiceInstanceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsLastArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PipelineCompletionTrackerTest {

    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";

    private static final String ZONE = "Europe/Paris";
    private final GetLastServiceOperationRequest pollingRequest = mock(GetLastServiceOperationRequest.class);
    private Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));
    private Request aFeignRequest = Request.create(Request.HttpMethod.GET, "https://url.domain", Collections.emptyMap(),
        Request.Body.empty(), new RequestTemplate());

    @TempDir
    File repositoryDirectory;

    private Path workDir;
    private OsbProxy osbProxy = mock(OsbProxy.class);

    private SecretsReader secretsReader = Mockito.mock(SecretsReader.class);
    private PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, 1200L, osbProxy, secretsReader);

    @BeforeEach
    public void setUp_SecretsReader_to_report_missing_manifest() {
        //Mockito happens to return false by default on boolean method, but let's be explicit so that
        //future refactorings would not break by accident
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(false);
    }

    @BeforeEach
    public void preparePaasSecretWorkDir() {
        workDir = repositoryDirectory.toPath();
    }

    @BeforeEach
    public void setUpOsbProxy() {
        doAnswer(returnsLastArg()).when(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void raises_exception_when_receiving_unsupported_operation_state() {
        //When
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> tracker.buildResponse("unsupported-class-name", true, false, 10000L,
                pollingRequest,
                aDeleteServiceInstanceRequest()));
        assertThat(runtimeException.getMessage()).isEqualTo("Get Deployment Execution status fails (unhandled request" +
            " class:unsupported-class-name)");
    }

    @Test
    public void returns_failed_state_if_provision_operation_state_is_timed_out() {
        //TODO : Test with null work dir
        //Given a missing manifest file and a create operation state in the past
        String jsonPipelineOperationState = createProvisionOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.FAILED);
        assertThat(response.getDescription()).startsWith("Execution timeout after ");
        verify(osbProxy, never()).delegateProvision(any(), any(), any());
    }

    @Test
    public void returns_succeeded_state_during_provision_if_manifest_is_present_regardless_of_elapsed_time()
        throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and a completion marker is present in manifest
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(aCoabVarsFileDto());
        String jsonPipelineOperationState = createProvisionOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("Creation succeeded");
        assertThat(response.isDeleteOperation()).isFalse();
        //and proxy is invoked
        verify(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void delegates_to_osb_proxy_when_provision_completes_with_manifest_being_present() throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and a completion marker is present in manifest
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(aCoabVarsFileDto());

        String jsonPipelineOperationState = createProvisionOperationStateInThePast();
        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = GetLastServiceOperationResponse.builder()
            .operationState(OperationState.SUCCEEDED)
            .description("osb proxied")
                .build();

        when(osbProxy.delegateProvision(any(), any(), any())).thenReturn(proxiedResponse);


        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void delegates_bind_to_osb_proxy_when_manifest_is_present() {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and
        CreateServiceInstanceBindingRequest request = aBindingRequest(SERVICE_INSTANCE_ID);
        when(osbProxy.delegateBind(any())).thenReturn(aBindingResponse());

        //When
        CreateServiceInstanceBindingResponse response = tracker.delegateBindRequest(workDir, request);

        //Then
        assertThat(response).isEqualTo(aBindingResponse());
        verify(osbProxy).delegateBind(request);
    }

    @Test
    public void delegates_unbind_to_osb_proxy_when_manifest_is_present() {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and
        DeleteServiceInstanceBindingRequest request = OsbBuilderHelper.anUnbindRequest(SERVICE_INSTANCE_ID, "service-binding-id");
        doNothing().when(osbProxy).delegateUnbind(any());

        //When
        tracker.delegateUnbindRequest(workDir, request);

        verify(osbProxy).delegateUnbind(request);
    }
    @Test
    public void rejects_bind_request_when_manifest_is_absent() {
        //Given no manifest file

        //When
        ServiceInstanceDoesNotExistException serviceInstanceDoesNotExistException = assertThrows(
            ServiceInstanceDoesNotExistException.class, () -> tracker.checkBindingRequestsPrereqs(workDir,
                SERVICE_INSTANCE_ID));
        //then
        assertThat(serviceInstanceDoesNotExistException).hasMessageContaining(SERVICE_INSTANCE_ID);
    }
    @Test
    public void rejects_bind_request_when_no_osb_proxy_configured() {
        //Given a null proxy was configured
        tracker = new PipelineCompletionTracker(clock, 1200L, null, secretsReader);
        //Given a manifest file is available
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);

        //When
        ServiceBrokerException serviceBrokerException = assertThrows(ServiceBrokerException.class,
            () -> tracker.checkBindingRequestsPrereqs(workDir,
                SERVICE_INSTANCE_ID));
        //Then
        assertThat(serviceBrokerException).hasMessageContaining("Bindings not supported for this service");
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_present() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .description("osb proxied")
                .build();
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);

        //When
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), true, false, 10, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateDeprovision(any(), any(), any());
    }

    @Test
    public void returns_success_state_on_osb_proxy_404_missing_catalog_response_when_deprovision_completes() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that can not reach the enclosing broker
        Response errorResponse = Response.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Requested route ('cassandra-broker_85329f8c-40d0-482b-a996-ff8bee2d4b1a.mydomain') does not exist.\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException catalogException = FeignException.errorStatus("CatalogServiceClient#getCatalog()", errorResponse);
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenThrow(catalogException );

        //When
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), true, false, 10, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.isDeleteOperation()).isTrue();
        assertThat(response.getDescription()).isNull();
    }

    @Test
    public void returns_failed_state_on_osb_proxy_404_missing_catalog_response_during_provision() {
        CreateServiceInstanceRequest request = OsbBuilderHelper.aCreateServiceInstanceRequest();

        //Given a proxy that can not reach the enclosing broker
        Response errorResponse = Response.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Requested route ('cassandra-broker_85329f8c-40d0-482b-a996-ff8bee2d4b1a.mydomain') does not exist.\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException catalogException = FeignException.errorStatus("CatalogServiceClient#getCatalog()", errorResponse);
        when(osbProxy.delegateProvision(any(), any(), any())).thenThrow(catalogException );

        //When
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), true, false, 10, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.FAILED);
        assertThat(response.getDescription()).isNull();
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_absent() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .description("osb proxied")
                .build();
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);

        //When invoked before timeout
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), false, false, 10L, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateDeprovision(any(), any(), any());
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_absent_after_timeout() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .description("osb proxied")
                .build();
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);

        //When invoked after timeout
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), false, true, 10L, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateDeprovision(any(), any(), any());
    }

// we dropped direct provisionning delegation from cassandra processor
//    @Test
//    public void delegates_deprovision_to_osb_proxy() throws IOException {
//        //given an incoming unprovision request
//        DeleteServiceInstanceRequest request = aDeleteServiceInstanceRequest();
//
//        //when
//        tracker.delegateDeprovisionRequest(request);
//        String jsonPipelineOperationState = createOperationStateInThePast(request);
//        //Given a proxy that returns a custom response message
//        DeleteServiceInstanceResponse proxiedResponse = new DeleteServiceInstanceResponse();
//
//        when(createServiceInstanceOsbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);
//
//
//        //When
//        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);
//
//        //Then
//        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
//        assertThat(response.getDescription()).isEqualTo("osb proxied");
//    }

    @Test
    public void returns_succeeded_state_if_completion_marker_matches_and_provision_operation_state_without_timeout()
        throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and a completion marker is present in manifest
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(aCoabVarsFileDto());
        //and a create operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.aCreateServiceInstanceRequest(),
            aCoabVarsFileDto());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Creation succeeded");
    }

    @Test
    public void returns_succeeded_state_if_completion_marker_matches_and_update_operation_state_without_timeout()
        throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and a completion marker is present in manifest
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(aCoabVarsFileDto());
        //and an update operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.anUpdateServiceInstanceRequest(),
            aCoabVarsFileDto());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Update succeeded");
    }

    @Test
    public void returns_inprogress_state_if_manifest_is_not_present_and_provision_operation_state_before_timeout() {
        //Given a missing manifest file and a create operation state without timeout
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(false);
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.aCreateServiceInstanceRequest(),
            aCoabVarsFileDto());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
    }

    @Test
    public void returns_inprogress_state_when_completion_marker_missing_and_provision_operation_state_before_timeout()
        throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and a completion marker is missing in manifest (e.g. paas-templates v48 service instance, prior to coab
        //update in v50)
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(null);
        //and a create operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.aCreateServiceInstanceRequest(),
            aCoabVarsFileDtoV2());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
    }

    @Test
    public void returns_inprogress_state_when_completion_marker_does_not_match_and_update_operation_state_before_timeout()
        throws IOException {
        //Given an existing manifest file
        Mockito.when(secretsReader.isBoshDeploymentAvailable(any(), any())).thenReturn(true);
        //and an older completion marker is present in manifest (e.g. before plan update)
        Mockito.when(secretsReader.getBoshDeploymentCompletionMarker(any(), any())).thenReturn(aCoabVarsFileDto());
        //and an update operation state without timeout
        String jsonPipelineOperationState =
            tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.anUpdateServiceInstanceRequest(),
            aCoabVarsFileDtoV2());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
    }



    @NotNull
    private CoabVarsFileDto aCoabVarsFileDto() {
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name="a-deployment-name";
        coabVarsFileDto.plan_id="plan_guid1";
        return coabVarsFileDto;
    }

    @NotNull
    private CoabVarsFileDto aCoabVarsFileDtoV2() {
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name="a-deployment-name";
        coabVarsFileDto.plan_id="plan_guid2";
        //We could also simulate previous value, but this does not bring much value/coverage here
        return coabVarsFileDto;
    }



    private String createProvisionOperationStateInThePast() {
        CreateServiceInstanceRequest request = OsbBuilderHelper.aCreateServiceInstanceRequest();
        return createOperationStateInThePast(request);
    }

    private String createDeprovisionOperationStateInThePast() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();
        return createOperationStateInThePast(request);
    }

    private String createOperationStateInThePast(ServiceBrokerRequest request) {
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2018-01-22T14:00:00.000Z",
            aCoabVarsFileDto().hashCode());

        //when
        PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), 1200L, mock(OsbProxy.class), Mockito.mock(SecretsReader.class));
        return tracker.formatAsJson(pipelineOperationState);
    }

    @Test
    public void operation_state_POJO_serializes_back_and_forth_to_json() {
        //given
        CreateServiceInstanceRequest originalRequest = OsbBuilderHelper.aCreateServiceInstanceRequest();
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(originalRequest, "2018-01-22T14:00:00.000Z",
            -23);

        //when
        String json = tracker.formatAsJson(pipelineOperationState);

        //then
        PipelineCompletionTracker.PipelineOperationState actualPipelineOperationState= tracker.parseFromJson(json);
        CreateServiceInstanceRequest actualServiceBrokerRequest = (CreateServiceInstanceRequest) actualPipelineOperationState.getServiceBrokerRequest();

        //then
        assertEquals(actualPipelineOperationState, pipelineOperationState);
        assertEquals(actualServiceBrokerRequest, originalRequest);
        //CreateServiceInstanceRequest.equals ignores transient fields that we need to preserve in our context
        assertEquals(actualServiceBrokerRequest.getServiceInstanceId(), originalRequest.getServiceInstanceId());
        assertEquals(actualServiceBrokerRequest.getServiceDefinitionId(), originalRequest.getServiceDefinitionId());
        assertEquals(actualPipelineOperationState.getCompletionMarkerHashcode(), pipelineOperationState.getCompletionMarkerHashcode());
    }

    @Test
    public void operation_state_POJO_serializes_excluding_heavy_catalog_details() {
        //given a request with service definition set by the SCOSB framework when receiving it
        CreateServiceInstanceRequest originalRequest = OsbBuilderHelper.aCreateServiceInstanceRequest();
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        originalRequest.setServiceDefinition(serviceDefinition);
        originalRequest.setPlan(serviceDefinition.getPlans().get(0));

        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(originalRequest, "2018-01-22T14:00:00.000Z",
            -23);

        //when
        String json = tracker.formatAsJson(pipelineOperationState);

        //then the resulting json does not contain catalog details about the service definition,
        // in particular metadata which can contain heaby inline image data
        assertThat(json).doesNotContain("plan_description");
        assertThat(json).doesNotContain("metadata");
    }


}
