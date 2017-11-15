package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareProcessor;
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

    private File tfStateFile;
    private Clock clock;

    public TerraformCompletionTracker(File tfStateFile, Clock clock) {
        this.tfStateFile = tfStateFile;
        this.clock = clock;

        gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
    }

    public GetLastServiceOperationResponse getModuleExecStatus(String moduleId) {

        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tfStateFile)))) {
            TerraformState tfState = gson.fromJson(reader, TerraformState.class);

            Map<String, TerraformState.Output> outputs = tfState.getOutputs();

            TerraformState.Output started = outputs.get(moduleId + ".started");
            TerraformState.Output completed = outputs.get(moduleId + ".completed");

            return mapOutputToStatus(started, completed);
        } catch (IOException e) {
            logger.error("unable to extract tfstate output from:" + tfStateFile, e);
            response.withDescription("Internal error checking service instance state");
        }

        response.withOperationState(OperationState.FAILED);
        return response;
    }

     GetLastServiceOperationResponse mapOutputToStatus(TerraformState.Output started, TerraformState.Output completed) {
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        if (started == null) {
            //terraform module invocation not yet received
            response.withOperationState(OperationState.IN_PROGRESS);
        } else if (completed == null) {
            //module invocation received, but module failed
            response.withOperationState(OperationState.FAILED);
        } else {
            //module completed
            response.withOperationState(OperationState.SUCCEEDED);
        }
        return response;
    }

    public String getCurrentDate() {
        Instant now = Instant.now(clock);
        return now.toString();
    }

    public long getElapsedTimeSecsSinceLastOperation(@SuppressWarnings("SameParameterValue") String lastOperationDate) {
        Instant start = Instant.parse(lastOperationDate);
        Instant now = Instant.now(clock);
        return start.until(now, ChronoUnit.SECONDS);
    }
}
