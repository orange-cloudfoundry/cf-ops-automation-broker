package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Created by ijly7474 on 04/01/18.
 */
public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    protected Clock clock;
    protected Path workDir;
    protected String serviceInstanceId;
    private static final String CREATE = "create";
    private static final String DELETE = "delete";
    private static final String UPDATE = "update";


    public PipelineCompletionTracker(Clock clock, Path workDir, String serviceInstanceId) {
        this.clock = clock;
        this.workDir = workDir;
        this.serviceInstanceId = serviceInstanceId;
    }

    public GetLastServiceOperationResponse getDeploymentExecStatus(String lastServiceOperation) {
        Path targetManifestFile = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.YML_SUFFIX);
        boolean isTargetManifestFilePresent = Files.exists(targetManifestFile);

        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        if (CREATE.equals(lastServiceOperation)){
            if (isTargetManifestFilePresent){
                response.withOperationState(OperationState.SUCCEEDED);
                response.withDescription("Creation is succeeded");
            }else{
                response.withOperationState(OperationState.IN_PROGRESS);
                response.withDescription("Creation is in progress");
            }

        }else if (UPDATE.equals(lastServiceOperation)){
            //Don't know what to do

        }else if (DELETE.equals(lastServiceOperation)) {
            if (isTargetManifestFilePresent){
                response.withOperationState(OperationState.IN_PROGRESS);
                response.withDescription("Deletion is in progress");
            }else{
                response.withOperationState(OperationState.SUCCEEDED);
                response.withDescription("Deletion is succeeded");
            }
        }
        return response;
    }

}
