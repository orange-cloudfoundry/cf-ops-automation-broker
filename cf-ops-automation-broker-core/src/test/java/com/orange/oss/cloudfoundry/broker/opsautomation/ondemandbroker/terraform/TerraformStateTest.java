package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TerraformStateTest {

    @Test
    public void parses_terraform_state_outputs() throws IOException {
        //given a reference format
        String tfStateFile = "/terraform/terraform-with-outputs.tfstate";

        //when parsing
        Gson gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
        TerraformState deserialized = gson.fromJson(TerraformModuleHelper.getDataFileReader(tfStateFile), TerraformState.class);

        //then it extracts properly fields
        ImmutableTerraformState.Builder builder = ImmutableTerraformState.builder();
        ImmutableModule rootModuleOutputs = ImmutableModule.builder()
                .modulePath("root")
                .putOutputs("3456",
                        ImmutableOutput.builder()
                                .type("string")
                                .value("success")
                                .build())
                .build();
        builder.addModules(rootModuleOutputs);
        ImmutableModule myModuleOutputs = ImmutableModule.builder()
                .modulePath("root/my-module")
                .putOutputs("status",
                        ImmutableOutput.builder()
                                .type("string")
                                .value("success")
                                .build())
                .build();
        builder.addModules(myModuleOutputs);


        assertThat(deserialized).isEqualTo(builder.build());

    }

}
