package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.servicebroker.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
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
    private OsbProxy createServiceInstanceOsbProxy = mock(OsbProxy.class);

    private PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, 1200L, createServiceInstanceOsbProxy);


    @Before
    public void preparePaasSecretWorkDir() throws IOException {
        File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
        workDir = file.toPath();
    }

    @Before
    public void setUpOsbProxy() {
        doAnswer(returnsLastArg()).when(createServiceInstanceOsbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void raises_exception_when_receiving_unsupported_operation_state() throws IOException {
        //Then
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Get Deployment Execution status fails (unhandled request class)");

        //When
        tracker.buildResponse("unsupported-class-name", true, false, 10000L, pollingRequest, aDeleteServiceInstanceRequest());
    }

    @Test
    public void returns_failed_state_if_provision_operation_state_is_timed_out() throws IOException {
        //TODO : Test with null work dir
        //Given a missing manifest file and a create operation state in the past
        String jsonPipelineOperationState = createProvisionOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.FAILED);
        assertThat(response.getDescription()).startsWith("Execution timeout after ");
        verify(createServiceInstanceOsbProxy, never()).delegateProvision(any(), any(), any());
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
        verify(createServiceInstanceOsbProxy).delegateProvision(any(), any(), any());
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

        when(createServiceInstanceOsbProxy.delegateProvision(any(), any(), any())).thenReturn(proxiedResponse);


        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(createServiceInstanceOsbProxy).delegateProvision(any(), any(), any());
    }

    @Test
    public void delegates_to_osb_proxy_when_deprovision_completes_with_manifest_being_present() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
        String jsonPipelineOperationState = createDeprovisionOperationStateInThePast();
        //Given a proxy that returns a custom response message
        GetLastServiceOperationResponse proxiedResponse = new GetLastServiceOperationResponse();
        proxiedResponse.withOperationState(OperationState.SUCCEEDED);
        proxiedResponse.withDescription("osb proxied");

        when(createServiceInstanceOsbProxy.delegateDeprovision(any(), any(), any())).thenReturn(proxiedResponse);


        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).isEqualTo("osb proxied");
        verify(createServiceInstanceOsbProxy).delegateDeprovision(any(), any(), any());
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
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Creation succeeded");
    }

    @Test
    public void returns_inprogress_state_if_manifest_is_not_present_and_provision_operation_state_before_timeout() throws IOException {
        //Given a missing manifest file and a create operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState, pollingRequest);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
        assertThat(response.getDescription()).describedAs("Creation is in progress");
    }


    
    private String createProvisionOperationStateInThePast() {
        CreateServiceInstanceRequest request = aCreateServiceInstanceRequest();
        return createOperationStateInThePast(request);
    }

    private String createDeprovisionOperationStateInThePast() {
        DeleteServiceInstanceRequest request = aDeleteServiceInstanceRequest();
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
        CreateServiceInstanceRequest originalRequest = aCreateServiceInstanceRequest();
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

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest(){
        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("service-instance-guid");
        return request;
    }

    private DeleteServiceInstanceRequest aDeleteServiceInstanceRequest() {
        // Given an incoming delete request
        return new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);
    }

    private UpdateServiceInstanceRequest anUpdateServiceInstanceRequest() {
        // Given an incoming delete request
        return new UpdateServiceInstanceRequest(
                "service_id",
                "plan_id",
                new HashMap<>())
                .withServiceInstanceId("instance_id");
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
