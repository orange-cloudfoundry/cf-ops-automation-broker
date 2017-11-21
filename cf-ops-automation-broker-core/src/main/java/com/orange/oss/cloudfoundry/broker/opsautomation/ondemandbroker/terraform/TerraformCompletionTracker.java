package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.io.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 *
 */
public class TerraformCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(TerraformCompletionTracker.class.getName());


    private Gson gson;

    private Clock clock;
    private int maxExecutionDurationSeconds;

    public TerraformCompletionTracker(Clock clock, int maxExecutionDurationSeconds) {
        this.clock = clock;
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;

        gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
    }

    public String getCurrentDate() {
        Instant now = Instant.now(clock);
        return now.toString();
    }

    public GetLastServiceOperationResponse getModuleExecStatus(File tfStateFile, String moduleName, String lastOperationState) {

        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfStateFile)))) {
            TerraformState tfState = gson.fromJson(reader, TerraformState.class);

            Map<String, TerraformState.Output> outputs = tfState.getOutputs();

            TerraformState.Output started = outputs.get(moduleName + ".started");
            TerraformState.Output completed = outputs.get(moduleName + ".completed");

            long elapsedTimeSecsSinceLastOperation = getElapsedTimeSecsSinceLastOperation(lastOperationState);
            return mapOutputToStatus(started, completed, elapsedTimeSecsSinceLastOperation);
        } catch (IOException e) {
            logger.error("unable to extract tfstate output from:" + tfStateFile, e);
            response.withDescription("Internal error checking service instance state");
        }

        response.withOperationState(OperationState.FAILED);
        return response;
    }

     GetLastServiceOperationResponse mapOutputToStatus(TerraformState.Output started, TerraformState.Output completed, long elapsedTimeSecsSinceLastOperation) {
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        if (started == null) {
            //terraform module invocation not yet received
            if (elapsedTimeSecsSinceLastOperation < maxExecutionDurationSeconds) {
                response.withOperationState(OperationState.IN_PROGRESS);
            } else {
                response.withOperationState(OperationState.FAILED);
                response.withDescription("execution timeout after " + elapsedTimeSecsSinceLastOperation +  "s max is " + maxExecutionDurationSeconds);
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

    long getElapsedTimeSecsSinceLastOperation(@SuppressWarnings("SameParameterValue") String lastOperationDate) {
        Instant start = Instant.parse(lastOperationDate);
        Instant now = Instant.now(clock);
        long elapsedSeconds = start.until(now, ChronoUnit.SECONDS);
        if (elapsedSeconds < 0) {
            logger.error("Unexpected operation date in future:" +lastOperationDate + " Is there a clock desynchronized around ?");
            //We don't know who's wrong so, so we don't trigger a service instance failure.
        }
        return elapsedSeconds;
    }
}
