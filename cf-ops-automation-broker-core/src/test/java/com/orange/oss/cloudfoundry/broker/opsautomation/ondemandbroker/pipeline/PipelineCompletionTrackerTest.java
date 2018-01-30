package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.util.FileSystemUtils;

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

public class PipelineCompletionTrackerTest {

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    public static final String REPOSITORY_DIRECTORY = "paas-secrets";
    public static final String ZONE = "Europe/Paris";
    private Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Path workDir;
    PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock);


    @Before
    public void preparePaasSecretWorkDir() throws IOException {
        workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
    }

    @After
    public void cleanUpPaasSecretWorkDir() {
        FileSystemUtils.deleteRecursively(workDir.toFile());
    }

    @Test
    public void raises_exception_when_receiving_unsupported_operation_state() throws IOException {
        //Then
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Get Deployment Execution status fails (unhandled request class)");

        //Given pipeline completion tracker with a "delete pipeline operation state"
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aDeleteServiceInstanceRequest());

        //When
        tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState);
    }

    @Test
    public void returns_failed_state_if_create_operation_state_is_timed_out() throws IOException {
        //TODO : Test with null work dir
        //Given a missing manifest file and a create operation state in the past
        String jsonPipelineOperationState = createOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.FAILED);
        assertThat(response.getDescription()).startsWith("execution timeout after ");
    }

    @Test
    public void returns_succeeded_state_if_manifest_is_present_regardless_of_elapsed_time() throws IOException {
        //Given an existing manifest file
        generateSampleManifest();
        String jsonPipelineOperationState = createOperationStateInThePast();

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Creation is succeeded");
    }

    @Test
    public void returns_succeeded_state_if_manifest_is_present_and_create_operation_state_without_timeout() throws IOException {
        //Given an existing manifest file and a create operation state without timeout
        generateSampleManifest();
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
        assertThat(response.getDescription()).describedAs("Creation is succeeded");
    }

    @Test
    public void returns_inprogress_state_if_manifest_is_not_present_and_create_operation_state_before_timeout() throws IOException {
        //Given a missing manifest file and a create operation state without timeout
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aCreateServiceInstanceRequest());

        //When
        GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, jsonPipelineOperationState);

        //Then
        assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
        assertThat(response.getDescription()).describedAs("Creation is in progress");
    }


    
    protected String createOperationStateInThePast() {
        return "{\"org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"parameterName\":\"parameterValue\"},\"asyncAccepted\":false},\"startRequestDate\":\"2018-01-22T14:00:00.000Z\"}";
    }

    @Test
    public void check_java2json_conversion() {
        //given
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState( aCreateServiceInstanceRequest(), "2018-01-22T14:00:00.000Z");

        //when
        PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC());
        String actualJson = tracker.formatAsJson(pipelineOperationState);
        System.out.println(actualJson);

        //then
        String expectedJson = "{\"org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"parameterName\":\"parameterValue\"},\"asyncAccepted\":false},\"startRequestDate\":\"2018-01-22T14:00:00.000Z\"}";
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void check_json2java_conversion() {
        //given //CreateServiceInstanceRequest
        String json = "{\"org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"parameterName\":\"parameterValue\"},\"asyncAccepted\":false},\"startRequestDate\":\"2018-01-22T14:00:00.000Z\"}";

        //when
        PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC());
        PipelineCompletionTracker.PipelineOperationState actualPipelineOperationState= tracker.parseFromJson(json);

        //then
        PipelineCompletionTracker.PipelineOperationState expectedPipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(aCreateServiceInstanceRequest(), "2018-01-22T14:00:00.000Z");
        assertEquals(actualPipelineOperationState, expectedPipelineOperationState);
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

    protected void generateSampleManifest() throws IOException {
        Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
        serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
        Path targetManifestFile = StructureGeneratorHelper.generatePath(serviceInstanceDir,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.YML_SUFFIX);
        Files.createFile(targetManifestFile);
    }


}
