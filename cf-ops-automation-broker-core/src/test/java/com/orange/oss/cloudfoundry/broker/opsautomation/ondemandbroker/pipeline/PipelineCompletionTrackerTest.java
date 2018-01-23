package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.fest.assertions.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

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


/**
 * Created by ijly7474 on 04/01/18.
 */
public class PipelineCompletionTrackerTest {

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    public static final String REPOSITORY_DIRECTORY = "paas-secrets";
    public static final String ZONE = "Europe/Paris";

    @Test
    public void stores_operation_state_in_a_json_serialized_pojo() {

    }

    @Test
    public void returns_succeeded_state_if_manifest_is_present_and_last_operation_is_create(){
        try {
            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
            serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
            Path targetManifestFile = StructureGeneratorHelper.generatePath(serviceInstanceDir,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.YML_SUFFIX);
            Files.createFile(targetManifestFile);
            Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

            //When
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, CassandraProcessorConstants.OSB_OPERATION_CREATE);

            //Then
            assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
            assertThat(response.getDescription()).describedAs("Creation is succeeded");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void returns_inprogress_state_if_manifest_is_not_present_and_last_operation_is_create(){
        try {
            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

            //When
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, CassandraProcessorConstants.OSB_OPERATION_CREATE);

            //Then
            assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
            assertThat(response.getDescription()).describedAs("Creation is in progress");

            //Then
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

    @Test@Ignore//Delete is now synchronous
    public void returns_succeeded_state_if_manifest_is_not_present_and_last_operation_is_delete(){
        try {
            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

            //When
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, CassandraProcessorConstants.OSB_OPERATION_DELETE);

            //Then
            assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
            assertThat(response.getDescription()).describedAs("Creation is suceeded");

            //Then
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test@Ignore //Delete is now synchronous
    public void returns_inprogress_state_if_manifest_is_present_and_last_operation_is_delete(){
        try {
            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
            serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
            Path targetManifestFile = StructureGeneratorHelper.generatePath(serviceInstanceDir,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.YML_SUFFIX);
            Files.createFile(targetManifestFile);
            Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

            //When
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(workDir, SERVICE_INSTANCE_ID, CassandraProcessorConstants.OSB_OPERATION_DELETE);

            //Then
            assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
            assertThat(response.getDescription()).describedAs("Creation is in progress");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_java2json_conversion() {
        //given
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState("2018-01-22T14:00:00.000Z", aCreateServiceInstanceRequest());

        //when
        PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC());
        String actualJson = tracker.formatAsJson(pipelineOperationState);
        System.out.println(actualJson);

        //then
        String expectedJson = "{\"createServiceInstanceRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"parameterName\":\"parameterValue\"},\"asyncAccepted\":false},\"lastOperationDate\":\"2018-01-22T14:00:00.000Z\"}";
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void check_json2java_conversion() {
        //given //CreateServiceInstanceRequest
        String json = "{\"createServiceInstanceRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"parameterName\":\"parameterValue\"},\"asyncAccepted\":false},\"lastOperationDate\":\"2018-01-22T14:00:00.000Z\"}";

        //when
        PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC());
        PipelineCompletionTracker.PipelineOperationState actualPipelineOperationState= tracker.parseFromJson(json);

        //then
        PipelineCompletionTracker.PipelineOperationState expectedPipelineOperationState = new PipelineCompletionTracker.PipelineOperationState("2018-01-22T14:00:00.000Z", aCreateServiceInstanceRequest());
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





}
