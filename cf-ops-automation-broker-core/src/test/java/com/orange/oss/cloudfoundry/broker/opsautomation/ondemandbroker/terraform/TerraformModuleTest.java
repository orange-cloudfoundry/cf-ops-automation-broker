package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TerraformModuleTest {

     @Test
    public void parses_and_serializes_terraform_module_json_file() throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
        TerraformModule terraformModule = gson.fromJson(getTestDataFileReader("terraform/cloudflare-serviceinstanceguid3456-route5.tf.json"), ImmutableTerraformModule.class);


        String serialized = gson.toJson(terraformModule);
        System.err.println(serialized);

        TerraformModule parsed = gson.fromJson(serialized, ImmutableTerraformModule.class);

        assertEquals(parsed, terraformModule);

    }

    @SuppressWarnings("SameParameterValue")
    static Reader getTestDataFileReader(String fileName) {
        return new BufferedReader(new InputStreamReader(TerraformModuleTest.class.getResourceAsStream("/" + fileName)));
    }
}
