package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.fest.assertions.Assertions;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;


/**
 * Created by ijly7474 on 04/01/18.
 */
public class PipelineCompletionTrackerTest {

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    public static final String REPOSITORY_DIRECTORY = "paas-secrets";
    public static final String ZONE = "Europe/Paris";

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
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, workDir, SERVICE_INSTANCE_ID);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(CassandraProcessorConstants.OSB_OPERATION_CREATE);

            //Then
            Assertions.assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
            Assertions.assertThat(response.getDescription()).describedAs("Creation is succeeded");
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
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, workDir, SERVICE_INSTANCE_ID);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(CassandraProcessorConstants.OSB_OPERATION_CREATE);

            //Then
            Assertions.assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
            Assertions.assertThat(response.getDescription()).describedAs("Creation is in progress");

            //Then
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

    @Test
    public void returns_succeeded_state_if_manifest_is_not_present_and_last_operation_is_delete(){
        try {
            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Clock clock = Clock.fixed(Instant.now(), ZoneId.of(ZONE));

            //When
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, workDir, SERVICE_INSTANCE_ID);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(CassandraProcessorConstants.OSB_OPERATION_DELETE);

            //Then
            Assertions.assertThat(response.getState()).isEqualTo(OperationState.SUCCEEDED);
            Assertions.assertThat(response.getDescription()).describedAs("Creation is suceeded");

            //Then
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
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
            PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, workDir, SERVICE_INSTANCE_ID);
            GetLastServiceOperationResponse response = tracker.getDeploymentExecStatus(CassandraProcessorConstants.OSB_OPERATION_DELETE);

            //Then
            Assertions.assertThat(response.getState()).isEqualTo(OperationState.IN_PROGRESS);
            Assertions.assertThat(response.getDescription()).describedAs("Creation is in progress");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
