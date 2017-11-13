package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Custom Gson adapter.
 * TODO: study whether immutables can simplify things https://immutables.github.io/json.html
 */
public class TerraformStateGsonAdapter implements JsonDeserializer<TerraformState> {


    public TerraformState deserialize(JsonElement json, Type type,
                                      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {


        ImmutableTerraformState.Builder outputsBuilder = ImmutableTerraformState.builder();
        JsonObject jsonObject = json.getAsJsonObject();
        JsonArray modules = jsonObject.getAsJsonArray("modules");
        for (JsonElement element : modules) {
            JsonObject module = (JsonObject) element;
            JsonArray pathElements = module.getAsJsonArray("path");
            if (pathElements.size() !=1) {
                continue; //only parse root module
            }

            JsonObject outputEntryObject = module.getAsJsonObject("outputs");
            Set<Map.Entry<String, JsonElement>> outputEntries = outputEntryObject.entrySet();
            for (Map.Entry<String, JsonElement> outputEntry : outputEntries) {
                String varName = outputEntry.getKey();
                JsonObject jsonVariable = (JsonObject) outputEntry.getValue();
                ImmutableOutput variable = ImmutableOutput.builder()
                        .value(jsonVariable.get("value").getAsString())
                        .type(jsonVariable.get("type").getAsString())
                        .build();
                outputsBuilder.putOutputs(varName, variable);
            }
        }
        return outputsBuilder.build();
    }

 
}
