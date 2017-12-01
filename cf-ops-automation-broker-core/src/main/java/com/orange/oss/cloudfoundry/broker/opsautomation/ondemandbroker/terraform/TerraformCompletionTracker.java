package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.io.*;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 *
 */
public class TerraformCompletionTracker {

    public static final String DELETE = "delete";
    public static final String CREATE = "create";
    private static Logger logger = LoggerFactory.getLogger(TerraformCompletionTracker.class.getName());
    private final String pathToTfState;


    private Gson gson;

    private Clock clock;
    private int maxExecutionDurationSeconds;

    public TerraformCompletionTracker(Clock clock, int maxExecutionDurationSeconds, String pathToTfState) {
        this.clock = clock;
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;
        this.pathToTfState = pathToTfState;

        gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
    }

    public String getOperationStateAsJson(String operationType) {
        TfOperationState tfOperationState = new TfOperationState(
                getCurrentDate(),
                operationType
        );
        return formatAsJson(tfOperationState);
    }

    public GetLastServiceOperationResponse getModuleExecStatus(Path gitWorkDir, String moduleName, String lastOperationState) {
        TfOperationState tfOperationState = parseFromJson(lastOperationState);
        boolean isDelete = DELETE.equals(tfOperationState.operation);
        File tfStateFile = gitWorkDir.resolve(pathToTfState).toFile();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfStateFile)))) {
            TerraformState tfState = gson.fromJson(reader, TerraformState.class);

            Map<String, TerraformState.Output> outputs = tfState.getOutputs();

            TerraformState.Output started = outputs.get(moduleName + ".started");
            TerraformState.Output completed = outputs.get(moduleName + ".completed");

            long elapsedTimeSecsSinceLastOperation = getElapsedTimeSecsSinceLastOperation(tfOperationState.lastOperationDate);
            return isDelete ?
                    mapOutputToDeletionStatus(started, completed, elapsedTimeSecsSinceLastOperation) :
                    mapOutputToCreationStatus(started, completed, elapsedTimeSecsSinceLastOperation);
        } catch (IOException e) {
            logger.error("unable to extract tfstate output from:" + tfStateFile, e);
            throw new RuntimeException("unable to check service instance status . Client platform should retry polling status");
        }
    }

    GetLastServiceOperationResponse mapOutputToCreationStatus(TerraformState.Output started, TerraformState.Output completed, long elapsedTimeSecsSinceLastOperation) {
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        if (started == null) {
            //terraform module invocation not yet received
            if (elapsedTimeSecsSinceLastOperation < maxExecutionDurationSeconds) {
                response.withOperationState(OperationState.IN_PROGRESS);
            } else {
                response.withOperationState(OperationState.FAILED);
                response.withDescription("execution timeout after " + elapsedTimeSecsSinceLastOperation + "s max is " + maxExecutionDurationSeconds);
            }
        } else if (completed == null) {
            //module invocation received, but module failed
            response.withOperationState(OperationState.FAILED);
        } else {
            //module completed
            response.withOperationState(OperationState.SUCCEEDED);
        }
        logger.info("Mapping started=" + started
                + " completed=" + completed
                + " elapsedTimeSecsSinceLastOperation=" + elapsedTimeSecsSinceLastOperation
                + " within maxExecutionDurationSeconds=" + maxExecutionDurationSeconds
                + " into:" + response);
        return response;
    }

    GetLastServiceOperationResponse mapOutputToDeletionStatus(TerraformState.Output started, TerraformState.Output completed, long elapsedTimeSecsSinceLastOperation) {
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        response.withDeleteOperation(true);
        if (started != null) {
            //module invocation pending
            if (elapsedTimeSecsSinceLastOperation < maxExecutionDurationSeconds) {
                response.withOperationState(OperationState.IN_PROGRESS);
            } else {
                response.withOperationState(OperationState.FAILED);
                response.withDescription("execution timeout after " + elapsedTimeSecsSinceLastOperation + "s max is " + maxExecutionDurationSeconds);
            }
        } else {
            if (completed != null) {
                response.withOperationState(OperationState.FAILED);
            } else {
                response.withOperationState(OperationState.SUCCEEDED);
            }
        }
        logger.info("Mapping started=" + started
                + " completed=" + completed
                + " elapsedTimeSecsSinceLastOperation=" + elapsedTimeSecsSinceLastOperation
                + " within maxExecutionDurationSeconds=" + maxExecutionDurationSeconds
                + " into:" + response);
        return response;
    }

    public String getCurrentDate() {
        Instant now = Instant.now(clock);
        return now.toString();
    }

    long getElapsedTimeSecsSinceLastOperation(@SuppressWarnings("SameParameterValue") String lastOperationDate) {
        Instant start = Instant.parse(lastOperationDate);
        Instant now = Instant.now(clock);
        long elapsedSeconds = start.until(now, ChronoUnit.SECONDS);
        if (elapsedSeconds < 0) {
            logger.error("Unexpected operation date in future:" + lastOperationDate + " Is there a clock desynchronized around ?");
            //We don't know who's wrong so, so we don't trigger a service instance failure.
        }
        return elapsedSeconds;
    }

    String formatAsJson(TfOperationState tfOperationState) {
        return gson.toJson(tfOperationState);
    }

    TfOperationState parseFromJson(String json) {
        return gson.fromJson(json, TfOperationState.class);
    }

    static class TfOperationState {
        private String lastOperationDate;
        private String operation;

        public TfOperationState() {
        }

        public TfOperationState(String lastOperationDate, String operation) {
            this.lastOperationDate = lastOperationDate;
            this.operation = operation;
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TfOperationState that = (TfOperationState) o;

            if (!lastOperationDate.equals(that.lastOperationDate)) return false;
            return operation.equals(that.operation);
        }

        @Override
        public int hashCode() {
            int result = lastOperationDate.hashCode();
            result = 31 * result + operation.hashCode();
            return result;
        }
    }
}
