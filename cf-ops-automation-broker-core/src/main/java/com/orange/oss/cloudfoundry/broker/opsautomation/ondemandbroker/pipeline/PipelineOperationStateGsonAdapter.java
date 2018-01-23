package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.google.gson.*;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Created by ijly7474 on 22/01/18.
 */
public class PipelineOperationStateGsonAdapter implements JsonDeserializer<PipelineCompletionTracker.PipelineOperationState>, JsonSerializer<PipelineCompletionTracker.PipelineOperationState> {

    //"{\"serviceBrokerRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"paramaterName\":\"paramaterValue\"},\"asyncAccepted\":false},\"lastOperationDate\":\"2018-01-22T14:00:00.000Z\",\"operation\":\"create\"}"


    @Override
    public JsonElement serialize(PipelineCompletionTracker.PipelineOperationState pipelineOperationState, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonObject = new JsonObject();
        ServiceBrokerRequest request = pipelineOperationState.getServiceBrokerRequest();
        String lastOperationDate = pipelineOperationState.getLastOperationDate();
        String classFullyQualifiedName = request.getClass().getName();
        JsonElement jsonElementRequest;
        switch(classFullyQualifiedName)
        {
            case "org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest":
                jsonElementRequest = jsonSerializationContext.serialize(request, CreateServiceInstanceRequest.class);
                jsonObject.add("createServiceInstanceRequest", jsonElementRequest);
                break;
            case "org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest":
                jsonElementRequest = jsonSerializationContext.serialize(request, DeleteServiceInstanceRequest.class);
                jsonObject.add("deleteServiceInstanceRequest", jsonElementRequest);
                break;

        }
        JsonElement jsonElementLastOperationDate = jsonSerializationContext.serialize(lastOperationDate, String.class);
        jsonObject.add("lastOperationDate", jsonElementLastOperationDate);

        return jsonObject;
    }

    @Override
    public PipelineCompletionTracker.PipelineOperationState deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        ServiceBrokerRequest serviceBrokerRequest = null;
        String lastOperationDate = null;
        for (Map.Entry<String, JsonElement> entry : entries) {
            String className = entry.getKey();
            switch(className)
            {
                case "createServiceInstanceRequest":
                serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), CreateServiceInstanceRequest.class);
                break;
                case "deleteServiceInstanceRequest":
                serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), DeleteServiceInstanceRequest.class);
                break;
                case "lastOperationDate":
                lastOperationDate = jsonDeserializationContext.deserialize(entry.getValue(), String.class);
                break;
            }
        }


        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(lastOperationDate, serviceBrokerRequest);

        return pipelineOperationState;
    }
}
