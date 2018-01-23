package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;


/**
 * Created by ijly7474 on 04/01/18.
 */
public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    protected Clock clock;
    private Gson gson;

    public PipelineCompletionTracker(Clock clock) {
        this.clock = clock;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PipelineCompletionTracker.PipelineOperationState.class, new PipelineOperationStateGsonAdapter());
        this.gson = gsonBuilder.create();
    }

    public String getOperationStateAsJson(ServiceBrokerRequest serviceBrokerRequest) {
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(
                getCurrentDate(),
                serviceBrokerRequest
        );
        return formatAsJson(pipelineOperationState);
    }

    public String getCurrentDate() {
        Instant now = Instant.now(clock);
        return now.toString();
    }



    public GetLastServiceOperationResponse getDeploymentExecStatus(Path workDir, String serviceInstanceId, String lastServiceOperation) {
        Path targetManifestFile = getTargetManifestFilePath(workDir, serviceInstanceId);
        boolean isTargetManifestFilePresent = Files.exists(targetManifestFile);

        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        if (CassandraProcessorConstants.OSB_OPERATION_CREATE.equals(lastServiceOperation)){
            if (isTargetManifestFilePresent){
                response.withOperationState(OperationState.SUCCEEDED);
                response.withDescription("Creation is succeeded");
            }else{
                response.withOperationState(OperationState.IN_PROGRESS);
                response.withDescription("Creation is in progress");
            }

        }else if (CassandraProcessorConstants.OSB_OPERATION_UPDATE.equals(lastServiceOperation)){
            //Don't know what to do
            throw new RuntimeException("update is currently unsupported");
        }/*else if (CassandraProcessorConstants.OSB_OPERATION_DELETE.equals(lastServiceOperation)) {
            if (isTargetManifestFilePresent){
                response.withOperationState(OperationState.IN_PROGRESS);
                response.withDescription("Deletion is in progress");
            }else{
                response.withOperationState(OperationState.SUCCEEDED);
                response.withDescription("Deletion is succeeded");
            }
        }TODO : Remove branch since delete is now synchronous*/
        return response;
    }

    public Path getTargetManifestFilePath(Path workDir, String serviceInstanceId) {
        return StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.YML_SUFFIX);
    }

    String formatAsJson(PipelineCompletionTracker.PipelineOperationState pipelineOperationState) {
        return gson.toJson(pipelineOperationState);
    }

    PipelineCompletionTracker.PipelineOperationState parseFromJson(String json) {
        return gson.fromJson(json, PipelineCompletionTracker.PipelineOperationState.class);
    }

    static class PipelineOperationState {
        private ServiceBrokerRequest serviceBrokerRequest;
        private String lastOperationDate;
        //private String operation;

        public PipelineOperationState() {
        }

        public PipelineOperationState(String lastOperationDate, ServiceBrokerRequest serviceBrokerRequest) {
            this.lastOperationDate = lastOperationDate;
            this.serviceBrokerRequest = serviceBrokerRequest;
            //this.operation = operation;
        }

        public ServiceBrokerRequest getServiceBrokerRequest(){
            return this.serviceBrokerRequest;
        }

        public String getLastOperationDate(){
            return this.lastOperationDate;
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            //TODO complete with ServiceBrokerRequest
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PipelineOperationState that = (PipelineOperationState) o;

            //if (!lastOperationDate.equals(that.lastOperationDate)) return false;
            //return operation.equals(that.operation);
            return lastOperationDate.equals(that.lastOperationDate);


        }

        @Override
        public int hashCode() {
            int result = 31 * lastOperationDate.hashCode();
            //result = 31 * result + operation.hashCode();
            return result;
        }
    }



}
