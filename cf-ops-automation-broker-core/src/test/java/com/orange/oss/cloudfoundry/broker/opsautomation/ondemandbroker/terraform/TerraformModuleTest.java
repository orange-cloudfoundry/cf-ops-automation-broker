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
        Gson gson = new GsonBuilder().registerTypeAdapter(TerraformModule.class, new TerraformModuleGsonAdapter()).create();
        TerraformModule terraformModule = gson.fromJson(getTestDataFileReader("terraform/cloudflare-serviceinstanceguid3456-route5.tf.json"), TerraformModule.class);


        String serialized = gson.toJson(terraformModule);
        System.err.println(serialized);

        TerraformModule parsed = gson.fromJson(serialized, TerraformModule.class);

        assertEquals(parsed, terraformModule);

    }

    static Reader getTestDataFileReader(String fileName) {
        return new BufferedReader(new InputStreamReader(TerraformModuleTest.class.getResourceAsStream("/" + fileName)));
    }
}
