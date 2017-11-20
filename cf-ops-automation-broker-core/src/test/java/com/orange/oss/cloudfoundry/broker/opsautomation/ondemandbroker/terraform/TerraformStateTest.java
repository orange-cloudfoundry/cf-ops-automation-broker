package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class TerraformStateTest {

    @Test
    public void parses_terraform_state_root_module_outputs() throws IOException {
        //given a reference format
        String tfStateFile = "/terraform/terraform-with-outputs.tfstate";

        //when parsing
        Gson gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
        TerraformState deserialized = gson.fromJson(TerraformModuleHelper.getDataFileReader(tfStateFile), TerraformState.class);

        //then it extracts properly fields
        ImmutableTerraformState terraformState = ImmutableTerraformState.builder()
                .putOutputs("3456",
                        ImmutableOutput.builder()
                                .type("string")
                                .value("success")
                                .build()).build();
        assertThat(deserialized).isEqualTo(terraformState);

    }
    @Test
    public void parses_multiple_terraform_state_root_module_outputs() throws IOException {
        //given a reference format
        String tfStateFile = "/terraform/terraform-with-successfull-module-exec.tfstate";

        //when parsing
        Gson gson = new GsonBuilder().registerTypeAdapter(TerraformState.class, new TerraformStateGsonAdapter()).create();
        TerraformState deserialized = gson.fromJson(TerraformModuleHelper.getDataFileReader(tfStateFile), TerraformState.class);

        //then it extracts properly fields similar to terraform output command:
        //4567.completed = successfully provisionned /tmp/writeable/file and /tmp/writeable/file
        //4567.started = successfully received module invocation

        ImmutableTerraformState terraformState = ImmutableTerraformState.builder()
                .putOutputs("4567.started",
                        ImmutableOutput.builder()
                                .type("string")
                                .value("successfully received module invocation")
                                .build())
                .putOutputs("4567.completed",
                        ImmutableOutput.builder()
                                .type("string")
                                .value("successfully provisionned /tmp/writeable/file and /tmp/writeable/file")
                                .build()).build();
        assertThat(deserialized).isEqualTo(terraformState);

    }

}
