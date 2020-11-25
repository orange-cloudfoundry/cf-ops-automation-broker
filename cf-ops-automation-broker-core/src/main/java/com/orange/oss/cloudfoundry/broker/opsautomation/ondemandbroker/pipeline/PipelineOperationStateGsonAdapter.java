package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

/**
 * Created by ijly7474 on 22/01/18.
 */
public class PipelineOperationStateGsonAdapter implements JsonDeserializer<PipelineCompletionTracker.PipelineOperationState>, JsonSerializer<PipelineCompletionTracker.PipelineOperationState> {

    //"{\"serviceBrokerRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"paramaterName\":\"paramaterValue\"},\"asyncAccepted\":false},\"startRequestDate\":\"2018-01-22T14:00:00.000Z\"}"


    @Override
    public JsonElement serialize(PipelineCompletionTracker.PipelineOperationState pipelineOperationState, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonObject = new JsonObject();
        ServiceBrokerRequest request = pipelineOperationState.getServiceBrokerRequest();
        String startRequestDate = pipelineOperationState.getStartRequestDate();
        String classFullyQualifiedName = request.getClass().getName();
        JsonElement jsonElementRequest;
        switch(classFullyQualifiedName)
        {
            case DeploymentConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                jsonElementRequest = jsonSerializationContext.serialize(request, CreateServiceInstanceRequest.class);
                jsonObject.add(DeploymentConstants.OSB_CREATE_REQUEST_CLASS_NAME, jsonElementRequest);
                break;
            case DeploymentConstants.OSB_UPDATE_REQUEST_CLASS_NAME:
                jsonElementRequest = jsonSerializationContext.serialize(request, UpdateServiceInstanceRequest.class);
                jsonObject.add(DeploymentConstants.OSB_UPDATE_REQUEST_CLASS_NAME, jsonElementRequest);
                break;
            case DeploymentConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                jsonElementRequest = jsonSerializationContext.serialize(request, DeleteServiceInstanceRequest.class);
                jsonObject.add(DeploymentConstants.OSB_DELETE_REQUEST_CLASS_NAME, jsonElementRequest);
                break;
            default:
                throw new RuntimeException("PipelineOperationStateGsonAdapter serialize method fails");
        }
        JsonElement jsonElementStartRequestDate = jsonSerializationContext.serialize(startRequestDate, String.class);
        jsonObject.add("startRequestDate", jsonElementStartRequestDate);
        jsonObject.add("completionMarkerHashcode",
            jsonSerializationContext.serialize(pipelineOperationState.getCompletionMarkerHashcode(), Integer.class));

        return jsonObject;
    }

    @Override
    public PipelineCompletionTracker.PipelineOperationState deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        ServiceBrokerRequest serviceBrokerRequest = null;
        String startRequestDate = null;
        int completionMarkerHashcode = 0;
        for (Map.Entry<String, JsonElement> entry : entries) {
            String entryKey = entry.getKey();
            switch(entryKey)
            {
                case DeploymentConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                    serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), CreateServiceInstanceRequest.class);
                    break;
                case DeploymentConstants.OSB_UPDATE_REQUEST_CLASS_NAME:
                    serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), UpdateServiceInstanceRequest.class);
                    break;
                case DeploymentConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                    serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), DeleteServiceInstanceRequest.class);
                    break;
                case "startRequestDate":
                    startRequestDate = jsonDeserializationContext.deserialize(entry.getValue(), String.class);
                    break;
                case "completionMarkerHashcode":
                    completionMarkerHashcode = jsonDeserializationContext.deserialize(entry.getValue(), Integer.class);
                    break;
            }
        }

        return new PipelineCompletionTracker.PipelineOperationState(serviceBrokerRequest, startRequestDate, completionMarkerHashcode);
    }
}
