package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TerraformModuleGsonAdapter implements JsonDeserializer<TerraformModule>, JsonSerializer<TerraformModule> {


    public TerraformModule deserialize(JsonElement json, Type type,
                                 JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {


        ImmutableTerraformModule.Builder terraformModuleBuilder = ImmutableTerraformModule.builder();

        JsonObject jsonObject = json.getAsJsonObject();
        JsonObject module = jsonObject.getAsJsonObject("module");
        Map.Entry<String, JsonElement> singleEntry = module.entrySet().iterator().next();
        String moduleName = singleEntry.getKey();
        terraformModuleBuilder.moduleName(moduleName);

        JsonObject moduleProperties = singleEntry.getValue().getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = moduleProperties.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();

            if ("source".equals(key)) {
                terraformModuleBuilder.source(value);
            } else {
                terraformModuleBuilder.putProperties(key, value);
            }
        }

        return terraformModuleBuilder.build();
    }

    public JsonElement serialize(TerraformModule src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleInvocation = new JsonObject();
        Set<Map.Entry<String, String>> entries = src.getProperties().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            moduleInvocation.addProperty(entry.getKey(), entry.getValue());
        }
        moduleInvocation.addProperty("source", src.getSource());

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(src.getModuleName(), moduleInvocation);

        JsonObject envelope = new JsonObject();
        envelope.add("module", jsonObject);
        return envelope;
    }
}
