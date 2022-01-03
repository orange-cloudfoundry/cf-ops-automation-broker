package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.util.Assert;


public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    private Clock clock;
    private OsbProxy osbProxy;
    private SecretsReader secretsReader;
    private Gson gson;
    private long maxExecutionDurationSeconds;

    public PipelineCompletionTracker(Clock clock, long maxExecutionDurationSeconds, OsbProxy osbProxy, SecretsReader secretsReader) {
        this.clock = clock;
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;
        this.osbProxy = osbProxy;
        this.secretsReader = secretsReader;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PipelineCompletionTracker.PipelineOperationState.class, new PipelineOperationStateGsonAdapter());

        this.gson = buildCustomGson(gsonBuilder);
    }

    private Gson buildCustomGson(GsonBuilder gsonBuilder) {
        // By default both static and transient fields are not serialized.
        // We need transient some fields from CreateServiceInstanceRequest to be serialized so we override this default
        // to only exclude static fields (such as constants)
        return gsonBuilder
                .excludeFieldsWithModifiers(Modifier.STATIC)
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        //Using class type to fail fast on SCOSB refactorings
                        if (ServiceDefinition.class.equals(f.getDeclaredClass())) {
                            return true;
                        }
                        //noinspection RedundantIfStatement
                        if (Plan.class.equals(f.getDeclaredClass())) {
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
    }

    GetLastServiceOperationResponse getDeploymentExecStatus(Path secretsWorkDir, String serviceInstanceId, String jsonPipelineOperationState, GetLastServiceOperationRequest pollingRequest) {
        PipelineOperationState pipelineOperationState = this.parseFromJson(jsonPipelineOperationState);
        if (pipelineOperationState == null) {
            logger.info("receiving /v2/service_instances/:instance_id/last_operation endpoint call without operation " +
                "field, assuming coab issue #467 and returning GONE");
            return GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .description("Unexpected null operation field, assuming issue #467")
                .deleteOperation(true)
                .build();
        }

        //Check if target manifest file is present, i.e. if nested broker bosh deployment completed successfully
        boolean isTargetManifestFilePresent = secretsReader.isBoshDeploymentAvailable(secretsWorkDir, serviceInstanceId);

        boolean isCompletionTrackerMachingRequest = false;
        if (isTargetManifestFilePresent) {
            CoabVarsFileDto boshDeploymentCompletionMarker = null;
            try {
                boshDeploymentCompletionMarker = secretsReader
                    .getBoshDeploymentCompletionMarker(secretsWorkDir, serviceInstanceId);
            }
            catch (IOException e) {
                logger.warn("Unable to parse COA bosh manifest file in dir {} for instance {} caught:",
                    secretsWorkDir, serviceInstanceId, e);
            }
            if (boshDeploymentCompletionMarker != null) {
                int boshDeploymentCompletionMarkerHashcode = boshDeploymentCompletionMarker.hashCode();
                logger.debug("loaded boshDeploymentCompletionMarker={}", boshDeploymentCompletionMarker);
                isCompletionTrackerMachingRequest =
                    (pipelineOperationState.completionMarkerHashcode == boshDeploymentCompletionMarkerHashcode);
                logger.debug("isCompletionTrackerMachingRequest={} as a result of Request completion tracker hashCode={} and manifest completion tracker hashCode={} ",
                    isCompletionTrackerMachingRequest,
                    pipelineOperationState.completionMarkerHashcode,
                    boshDeploymentCompletionMarkerHashcode);
            } else {
                logger.debug("Did not find yet boshDeploymentCompletionMarker in bosh manifest for " +
                    "serviceInstanceId={} Might be a service instance provisioned before coab 0.33 version.",
                    serviceInstanceId);
            }
        }

        //Check if timeout is reached
        long elapsedTimeSecsSinceStartRequestDate = this.getElapsedTimeSecsSinceStartRequestDate(pipelineOperationState.getStartRequestDate());
        boolean isRequestTimedOut = this.isRequestTimedOut(elapsedTimeSecsSinceStartRequestDate);

        //Build response based on the appropriate values and return it
        ServiceBrokerRequest serviceBrokerRequest = pipelineOperationState.getServiceBrokerRequest();
        String classFullyQualifiedName = serviceBrokerRequest.getClass().getName();
        return this.buildResponse(classFullyQualifiedName, isCompletionTrackerMachingRequest, isRequestTimedOut, elapsedTimeSecsSinceStartRequestDate, pollingRequest, serviceBrokerRequest);
    }


    GetLastServiceOperationResponse buildResponse(String classFullyQualifiedName, boolean isCompletionTrackerMachingRequest, boolean isRequestTimedOut, long displayedElapsedTimeSecs, GetLastServiceOperationRequest pollingRequest, ServiceBrokerRequest storedRequest) {
        GetLastServiceOperationResponse.GetLastServiceOperationResponseBuilder responseBuilder = GetLastServiceOperationResponse.builder();
        GetLastServiceOperationResponse response = null;
            switch (classFullyQualifiedName) {
                case DeploymentConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                    if (isCompletionTrackerMachingRequest) {
                        responseBuilder.operationState(OperationState.SUCCEEDED);
                        responseBuilder.description("Creation succeeded");
                        if (osbProxy != null) {
                            try {
                                response = osbProxy.delegateProvision(pollingRequest, (CreateServiceInstanceRequest) storedRequest, responseBuilder.build());
                            } catch (Exception e) {
                                logger.warn("Caught during provision delegation. Hint: if meeting 404 broker endpoint, check configuration mismatch between broker url endpoint property, and deployment bosh template", e);
                                responseBuilder.operationState(OperationState.FAILED);
                                responseBuilder.description(null);
                            }
                        }
                    } else {
                        if (isRequestTimedOut) {
                            responseBuilder.operationState(OperationState.FAILED);
                            responseBuilder.description("Execution timeout after " + displayedElapsedTimeSecs + "s max is " + maxExecutionDurationSeconds);
                        } else {
                            responseBuilder.operationState(OperationState.IN_PROGRESS);
                        }
                    }
                    break;

                case DeploymentConstants.OSB_UPDATE_REQUEST_CLASS_NAME:
                    if (isCompletionTrackerMachingRequest) {
                        responseBuilder.operationState(OperationState.SUCCEEDED);
                        responseBuilder.description("Update succeeded");
                        logger.debug("Updates don't yet get propagated to inner broker, proceeding with returning success response");
                    } else {
                        if (isRequestTimedOut) {
                            responseBuilder.operationState(OperationState.FAILED);
                            responseBuilder.description("Execution timeout after " + displayedElapsedTimeSecs + "s max is " + maxExecutionDurationSeconds);
                        } else {
                            responseBuilder.operationState(OperationState.IN_PROGRESS);
                        }
                    }
                    break;
                case DeploymentConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                    responseBuilder.deleteOperation(true);
                    if (osbProxy != null) {
                        try {
                            response = osbProxy.delegateDeprovision(pollingRequest, (DeleteServiceInstanceRequest) storedRequest, responseBuilder.build());
                        } catch (Exception e) {
                            logger.info("Unable to delegate delete to enclosed broker, maybe absent/down. Reporting as GONE. Caught:" + e, e);
                            responseBuilder.operationState(OperationState.SUCCEEDED);
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Get Deployment Execution status fails (unhandled request class:" + classFullyQualifiedName + ")");
            }
            if (response == null) {
                response = responseBuilder.build();
            }
        return response;
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

    String getPipelineOperationStateAsJson(ServiceBrokerRequest serviceBrokerRequest, CoabVarsFileDto coabVarsFileDto) {
        Assert.notNull(coabVarsFileDto, "provide a dummy coabars object, e.g. on delete");

        int completionMarkerHashcode = coabVarsFileDto.hashCode();
        logger.debug("Computed completionMarkerHashcode={} from OSB request wrapped in coabVarsFileDto={}",
            completionMarkerHashcode, coabVarsFileDto);
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(
                serviceBrokerRequest,
                getCurrentDate(), completionMarkerHashcode

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
        boolean boshDeploymentAvailable = secretsReader.isBoshDeploymentAvailable(secretsWorkDir, serviceInstanceId);

        if (!boshDeploymentAvailable) {
            logger.warn("Received bind/unbind request for a deployment not available, i.e. whose manifest is not " +
                "present in secret for id {}", serviceInstanceId);
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }
        if (osbProxy == null) {
            throw new ServiceBrokerException("Bindings not supported for this service");
        }
    }

    static class PipelineOperationState {
        private final ServiceBrokerRequest serviceBrokerRequest;
        private final String startRequestDate;
        private final int completionMarkerHashcode;

        PipelineOperationState(ServiceBrokerRequest serviceBrokerRequest, String startRequestDate,
            int completionMarkerHashcode) {
            this.serviceBrokerRequest = serviceBrokerRequest;
            this.startRequestDate = startRequestDate;
            this.completionMarkerHashcode = completionMarkerHashcode;
        }

        ServiceBrokerRequest getServiceBrokerRequest(){
            return this.serviceBrokerRequest;
        }

        String getStartRequestDate(){
            return this.startRequestDate;
        }

        public int getCompletionMarkerHashcode() {
            return completionMarkerHashcode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PipelineOperationState)) return false;

            PipelineOperationState that = (PipelineOperationState) o;

            if (completionMarkerHashcode != that.completionMarkerHashcode) return false;
            if (!serviceBrokerRequest.equals(that.serviceBrokerRequest)) return false;
            return startRequestDate.equals(that.startRequestDate);
        }

        @Override
        public int hashCode() {
            int result = serviceBrokerRequest.hashCode();
            result = 31 * result + startRequestDate.hashCode();
            result = 31 * result + completionMarkerHashcode;
            return result;
        }

    }



}
