package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 *
 */
public class TerraformModuleHelper {

    public static Reader getDataFileReader(String classpathResourceName) {
        return new BufferedReader(new InputStreamReader(TerraformModuleHelper.class.getResourceAsStream(classpathResourceName)));
    }

    public static  TerraformModule getTerraformModuleFromClasspath(String jsonFilePath) {
        Gson gson = getGson();
        return gson.fromJson(getDataFileReader(jsonFilePath), ImmutableTerraformModule.class);
    }

    public static Gson getGson() {
        return new GsonBuilder().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
    }
}
