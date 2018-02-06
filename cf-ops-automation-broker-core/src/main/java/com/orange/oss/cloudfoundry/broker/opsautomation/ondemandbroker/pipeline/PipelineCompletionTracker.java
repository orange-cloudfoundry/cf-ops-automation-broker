package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    protected Clock clock;
    private OsbProxy<CreateServiceInstanceRequest> createServiceInstanceOsbProxy;
    private Gson gson;
    private long maxExecutionDurationSeconds = 1200L;

    public PipelineCompletionTracker(Clock clock, OsbProxy<CreateServiceInstanceRequest> createServiceInstanceOsbProxy) {
        this.clock = clock;
        this.createServiceInstanceOsbProxy = createServiceInstanceOsbProxy;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PipelineCompletionTracker.PipelineOperationState.class, new PipelineOperationStateGsonAdapter());
        this.gson = gsonBuilder.create();
    }

    public GetLastServiceOperationResponse getDeploymentExecStatus(Path secretsWorkDir, String serviceInstanceId, String jsonPipelineOperationState, GetLastServiceOperationRequest pollingRequest) {

        //Check if target manifest file is present
        Path targetManifestFile = this.getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        boolean isTargetManifestFilePresent = Files.exists(targetManifestFile);

        //Check if timeout is reached
        PipelineOperationState pipelineOperationState = this.parseFromJson(jsonPipelineOperationState);
        long elapsedTimeSecsSinceStartRequestDate = this.getElapsedTimeSecsSinceStartRequestDate(pipelineOperationState.getStartRequestDate());
        boolean isRequestTimedOut = this.isRequestTimedOut(elapsedTimeSecsSinceStartRequestDate);

        //Build response based on the appropriate values and return it
        ServiceBrokerRequest serviceBrokerRequest = pipelineOperationState.getServiceBrokerRequest();
        String classFullyQualifiedName = serviceBrokerRequest.getClass().getName();
        return this.buildResponse(classFullyQualifiedName, isTargetManifestFilePresent, isRequestTimedOut, elapsedTimeSecsSinceStartRequestDate, pollingRequest, serviceBrokerRequest);
    }

    private GetLastServiceOperationResponse buildResponse(String classFullyQualifiedName, boolean asyncTaskCompleted, boolean isRequestTimedOut, long elapsedTimeSecsSinceStartRequestDate, GetLastServiceOperationRequest pollingRequest, ServiceBrokerRequest storedRequest){
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        switch(classFullyQualifiedName)
        {
            case CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                if (asyncTaskCompleted){
                    response.withOperationState(OperationState.SUCCEEDED);
                    response.withDescription("Creation is succeeded");
                    if (createServiceInstanceOsbProxy != null) {
                        return createServiceInstanceOsbProxy.delegate(pollingRequest, (CreateServiceInstanceRequest) storedRequest, response);
                    }

                }else{
                    if (isRequestTimedOut){
                        response.withOperationState(OperationState.FAILED);
                        response.withDescription("Execution timeout after " + elapsedTimeSecsSinceStartRequestDate + "s max is " + maxExecutionDurationSeconds);
                    }else {
                        response.withOperationState(OperationState.IN_PROGRESS);
                        response.withDescription("Creation is in progress");
                    }
                }
                break;
            default:
                throw new RuntimeException("Get Deployment Execution status fails (unhandled request class)");
        }
        return response;
    }

    public Path getTargetManifestFilePath(Path workDir, String serviceInstanceId) {
        return StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.YML_SUFFIX);
    }

    private String getCurrentDate() {
        return Instant.now(clock).toString();
    }

    private boolean isRequestTimedOut(long elapsedTimeSecsSinceStartRequestDate){
        return elapsedTimeSecsSinceStartRequestDate >= this.maxExecutionDurationSeconds;
    }

    private long getElapsedTimeSecsSinceStartRequestDate(String startRequestDate) {
        Instant start = Instant.parse(startRequestDate);
        Instant now = Instant.now(clock);
        long elapsedSeconds = start.until(now, ChronoUnit.SECONDS);
        if (elapsedSeconds < 0) {
            logger.error("Unexpected start request date in future:" + startRequestDate + " Is there a clock desynchronized around ?");
            //We don't know who's wrong so, so we don't trigger a service instance failure.
        }
        return elapsedSeconds;
    }

    public String getPipelineOperationStateAsJson(ServiceBrokerRequest serviceBrokerRequest) {
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(
                serviceBrokerRequest,
                getCurrentDate()

        );
        return formatAsJson(pipelineOperationState);
    }

    String formatAsJson(PipelineCompletionTracker.PipelineOperationState pipelineOperationState) {
        return gson.toJson(pipelineOperationState);
    }

    PipelineCompletionTracker.PipelineOperationState parseFromJson(String json) {
        return gson.fromJson(json, PipelineCompletionTracker.PipelineOperationState.class);
    }

    static class PipelineOperationState {
        private ServiceBrokerRequest serviceBrokerRequest;
        private String startRequestDate;

        public PipelineOperationState(ServiceBrokerRequest serviceBrokerRequest, String startRequestDate) {
            this.serviceBrokerRequest = serviceBrokerRequest;
            this.startRequestDate = startRequestDate;
        }

        public ServiceBrokerRequest getServiceBrokerRequest(){
            return this.serviceBrokerRequest;
        }

        public String getStartRequestDate(){
            return this.startRequestDate;
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PipelineOperationState that = (PipelineOperationState) o;

            if (!startRequestDate.equals(that.startRequestDate)) return false;
            return serviceBrokerRequest.equals(that.serviceBrokerRequest);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public int hashCode() {
            int result = 31 * startRequestDate.hashCode();
            return result;
        }
    }



}
