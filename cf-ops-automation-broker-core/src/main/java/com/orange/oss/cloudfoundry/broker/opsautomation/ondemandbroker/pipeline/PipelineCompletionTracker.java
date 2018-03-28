package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.*;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    private Clock clock;
    private OsbProxy osbProxy;
    private Gson gson;
    private long maxExecutionDurationSeconds;

    public PipelineCompletionTracker(Clock clock, long maxExecutionDurationSeconds, OsbProxy osbProxy) {
        this.clock = clock;
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;
        this.osbProxy = osbProxy;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PipelineCompletionTracker.PipelineOperationState.class, new PipelineOperationStateGsonAdapter());

        this.gson = buildCustomGson(gsonBuilder);
    }

    private Gson buildCustomGson(GsonBuilder gsonBuilder) {
        // By default both static and transient fields are not serialized.
        // We need transient fields from CreateServiceInstanceRequest to be serialized so we override this default
        // to only exclude static fields (such as constants)
        return gsonBuilder
                .excludeFieldsWithModifiers(Modifier.STATIC)
                .create();
    }

    GetLastServiceOperationResponse getDeploymentExecStatus(Path secretsWorkDir, String serviceInstanceId, String jsonPipelineOperationState, GetLastServiceOperationRequest pollingRequest) {

        //Check if target manifest file is present, i.e. if nested broker bosh deployment completed successfully
        boolean isTargetManifestFilePresent = isBoshDeploymentAvailable(secretsWorkDir, serviceInstanceId);

        //Check if timeout is reached
        PipelineOperationState pipelineOperationState = this.parseFromJson(jsonPipelineOperationState);
        long elapsedTimeSecsSinceStartRequestDate = this.getElapsedTimeSecsSinceStartRequestDate(pipelineOperationState.getStartRequestDate());
        boolean isRequestTimedOut = this.isRequestTimedOut(elapsedTimeSecsSinceStartRequestDate);

        //Build response based on the appropriate values and return it
        ServiceBrokerRequest serviceBrokerRequest = pipelineOperationState.getServiceBrokerRequest();
        String classFullyQualifiedName = serviceBrokerRequest.getClass().getName();
        return this.buildResponse(classFullyQualifiedName, isTargetManifestFilePresent, isRequestTimedOut, elapsedTimeSecsSinceStartRequestDate, pollingRequest, serviceBrokerRequest);
    }

    private boolean isBoshDeploymentAvailable(Path secretsWorkDir, String serviceInstanceId) {
        Path targetManifestFile = this.getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        boolean exists = Files.exists(targetManifestFile);
        logger.debug("Manifest at path {} exists: {}", targetManifestFile, exists);
        return exists;
    }


    GetLastServiceOperationResponse buildResponse(String classFullyQualifiedName, boolean isManifestFilePresent, boolean isRequestTimedOut, long displayedElapsedTimeSecs, GetLastServiceOperationRequest pollingRequest, ServiceBrokerRequest storedRequest) {
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
            switch (classFullyQualifiedName) {
                case CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                    if (isManifestFilePresent) {
                        response.withOperationState(OperationState.SUCCEEDED);
                        response.withDescription("Creation succeeded");
                        if (osbProxy != null) {
                            try {
                                response = osbProxy.delegateProvision(pollingRequest, (CreateServiceInstanceRequest) storedRequest, response);
                            } catch (Exception e) {
                                logger.warn("Caught during provision delegation. Hint: if meeting 404 broker endpoint, check configuration mismatch between broker url endpoint property, and deployment bosh template", e);
                                response.withOperationState(OperationState.FAILED);
                                response.withDescription(null);
                            }
                        }
                    } else {
                        if (isRequestTimedOut) {
                            response.withOperationState(OperationState.FAILED);
                            response.withDescription("Execution timeout after " + displayedElapsedTimeSecs + "s max is " + maxExecutionDurationSeconds);
                        } else {
                            response.withOperationState(OperationState.IN_PROGRESS);
                        }
                    }
                    break;

                case CassandraProcessorConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                    response.withDeleteOperation(true);
                    if (osbProxy != null) {
                        try {
                            response = osbProxy.delegateDeprovision(pollingRequest, (DeleteServiceInstanceRequest) storedRequest, response);
                        } catch (Exception e) {
                            logger.info("Unable to delegate delete to enclosed broker, maybe absent/down. Reporting as GONE. Caught:" + e, e);
                            response.withOperationState(OperationState.FAILED);
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

    String getPipelineOperationStateAsJson(ServiceBrokerRequest serviceBrokerRequest) {
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

    CreateServiceInstanceBindingResponse delegateBindRequest(Path secretsWorkDir, CreateServiceInstanceBindingRequest request) {
        checkBindingRequestsPrereqs(secretsWorkDir, request.getServiceInstanceId());
        return osbProxy.delegateBind(request);
    }

    void delegateUnbindRequest(Path secretsWorkDir, DeleteServiceInstanceBindingRequest request) {
        checkBindingRequestsPrereqs(secretsWorkDir, request.getServiceInstanceId());
        osbProxy.delegateUnbind(request);
    }

    void checkBindingRequestsPrereqs(Path secretsWorkDir, String serviceInstanceId) {
        //Check if target manifest file is present, i.e. if nested broker bosh deployment completed successfully
        boolean boshDeploymentAvailable = isBoshDeploymentAvailable(secretsWorkDir, serviceInstanceId);

        if (!boshDeploymentAvailable) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }
        if (osbProxy == null) {
            throw new ServiceBrokerException("Bindings not supported for this service");
        }
    }

    static class PipelineOperationState {
        private ServiceBrokerRequest serviceBrokerRequest;
        private String startRequestDate;

        PipelineOperationState(ServiceBrokerRequest serviceBrokerRequest, String startRequestDate) {
            this.serviceBrokerRequest = serviceBrokerRequest;
            this.startRequestDate = startRequestDate;
        }

        ServiceBrokerRequest getServiceBrokerRequest(){
            return this.serviceBrokerRequest;
        }

        String getStartRequestDate(){
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
