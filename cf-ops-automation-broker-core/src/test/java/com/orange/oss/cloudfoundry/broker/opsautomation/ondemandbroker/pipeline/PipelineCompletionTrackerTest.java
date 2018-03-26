package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import feign.FeignException;
import feign.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingRequest;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingResponse;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsLastArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PipelineCompletionTrackerTest {

    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private static final String REPOSITORY_DIRECTORY = "paas-secrets";
    private static final String ZONE = "Europe/Paris";
    private final GetLastServiceOperationRequest pollingRequest = mock(GetLastServiceOperationRequest.class);
    private Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path workDir;
    @SuppressWarnings("unchecked")
    private OsbProxy osbProxy = mock(OsbProxy.class);

    private PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, 1200L, osbProxy);


    @Before
    public void preparePaasSecretWorkDir() throws IOException {
        File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
        workDir = file.toPath();
    }

    @Before
    public void setUpOsbProxy() {
        doAnswer(returnsLastArg()).when(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void raises_exception_when_receiving_unsupported_operation_state() {
        //Then
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Get Deployment Execution status fails (unhandled request class)");

        //When
        tracker.buildResponse("unsupported-class-name", true, false, 10000L, pollingRequest, OsbBuilderHelper.aDeleteServiceInstanceRequest());
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
    public void returns_succeeded_state_if_manifest_is_present_regardless_of_elapsed_time() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
        String jsonPipelineOperationState = createProvisionOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("Creation succeeded");
        //and proxy is invoked
        verify(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void delegates_to_osb_proxy_when_provision_completes_with_manifest_being_present() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
        String jsonPipelineOperationState = createProvisionOperationStateInThePast();
        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = new GetLastServiceOperationResponse();
        proxiedResponse.withOperationState(OperationState.SUCCEEDED);
        proxiedResponse.withDescription("osb proxied");

        when(osbProxy.delegateProvision(any(), any(), any())).thenReturn(proxiedResponse);


        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void delegates_bind_to_osb_proxy_when_manifest_is_present() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
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
    public void delegates_unbind_to_osb_proxy_when_manifest_is_present() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
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

        thrown.expect(ServiceInstanceDoesNotExistException.class);
        thrown.expectMessage(containsString(SERVICE_INSTANCE_ID));

        //When
        tracker.checkBindingRequestsPrereqs(workDir, SERVICE_INSTANCE_ID);
    }
    @Test
    public void rejects_bind_request_when_no_osb_proxy_configured() throws IOException {
        //Given a null proxy was configured
        tracker = new PipelineCompletionTracker(clock, 1200L, null);
        //Given a manifest file
        generateSampleManifest();

        thrown.expect(ServiceBrokerException.class);
        thrown.expectMessage(containsString("Bindings not supported for this service"));

        //When
        tracker.checkBindingRequestsPrereqs(workDir, SERVICE_INSTANCE_ID);
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_present() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = new GetLastServiceOperationResponse();
        proxiedResponse.withOperationState(OperationState.SUCCEEDED);
        proxiedResponse.withDescription("osb proxied");
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);

        //When
        GetLastServiceOperationResponse response = tracker.buildResponse(request.getClass().getName(), true, false, (long) 10, pollingRequest, request);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(osbProxy).delegateDeprovision(any(), any(), any());
    }

    @Test(expected = ServiceInstanceDoesNotExistException.class)
    public void returns_410_GONE_on_osb_proxy_404_missing_catalog_response_when_deprovision_completes() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that can not reach the enclosing broker
        Response errorResponse = Response.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Requested route ('cassandra-broker_85329f8c-40d0-482b-a996-ff8bee2d4b1a.mydomain') does not exist.\"}", Charset.defaultCharset())
                .build();
        FeignException catalogException = FeignException.errorStatus("CatalogServiceClient#getCatalog()", errorResponse);
        when(osbProxy.delegateDeprovision(any(), any(), any())).thenThrow(catalogException );

        //When
        tracker.buildResponse(request.getClass().getName(), false, false, (long) 10, pollingRequest, request);

        //Then
        verify(osbProxy).delegateDeprovision(any(), any(), any());
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_absent() {
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = new GetLastServiceOperationResponse();
        proxiedResponse.withOperationState(OperationState.SUCCEEDED);
        proxiedResponse.withDescription("osb proxied");
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
        GetLastServiceOperationResponse proxiedResponse = new GetLastServiceOperationResponse();
        proxiedResponse.withOperationState(OperationState.SUCCEEDED);
        proxiedResponse.withDescription("osb proxied");
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
    public void returns_succeeded_state_if_manifest_is_present_and_provision_operation_state_without_timeout() throws IOException {
        //Given an existing manifest file and a create operation state without timeout
        generateSampleManifest();
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Creation succeeded");
    }

    @Test
    public void returns_inprogress_state_if_manifest_is_not_present_and_provision_operation_state_before_timeout() {
        //Given a missing manifest file and a create operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(OsbBuilderHelper.aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
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
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2018-01-22T14:00:00.000Z");

        //when
        @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), 1200L, mock(OsbProxy.class));
        return tracker.formatAsJson(pipelineOperationState);
    }

    @Test
    public void operation_state_POJO_serializes_back_and_forth_to_json() {
        //given
        CreateServiceInstanceRequest originalRequest = OsbBuilderHelper.aCreateServiceInstanceRequest();
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(originalRequest, "2018-01-22T14:00:00.000Z");

        //when
        @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), 1200L, mock(OsbProxy.class));
        String json = tracker.formatAsJson(pipelineOperationState);
        System.out.println(json);

        //then
        PipelineCompletionTracker.PipelineOperationState actualPipelineOperationState= tracker.parseFromJson(json);
        CreateServiceInstanceRequest actualServiceBrokerRequest = (CreateServiceInstanceRequest) actualPipelineOperationState.getServiceBrokerRequest();

        //then
        assertEquals(actualPipelineOperationState, pipelineOperationState);
        //CreateServiceInstanceRequest.equals ignores transient fields that we need to preserve in our context
        assertEquals(actualServiceBrokerRequest.getServiceInstanceId(), originalRequest.getServiceInstanceId());
        assertEquals(actualServiceBrokerRequest.getServiceDefinitionId(), originalRequest.getServiceDefinitionId());
    }


    private void generateSampleManifest() throws IOException {
        Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
        serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
        Path targetManifestFile = StructureGeneratorHelper.generatePath(serviceInstanceDir,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.YML_SUFFIX);
        Files.createFile(targetManifestFile);
    }


}
