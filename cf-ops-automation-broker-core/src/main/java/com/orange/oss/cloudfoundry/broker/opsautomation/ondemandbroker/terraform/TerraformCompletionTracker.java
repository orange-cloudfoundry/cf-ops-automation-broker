package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.io.*;
import java.util.Map;

/**
 *
 */
public class TerraformCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(TerraformCompletionTracker.class.getName());


    private Gson gson;

    private File tfStateFile;

    public TerraformCompletionTracker(File tfStateFile) {
        this.tfStateFile = tfStateFile;

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
}
