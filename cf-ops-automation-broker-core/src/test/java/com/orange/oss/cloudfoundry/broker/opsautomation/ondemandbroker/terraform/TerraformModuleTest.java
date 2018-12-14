package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TerraformModuleTest {

    @Test
    public void parses_and_serializes_terraform_module_json_file() throws IOException {
        //given a reference format
        String referenceJsonFormat = "/terraform/cloudflare-serviceinstanceguid3456-route5.tf.json";

        //when parsing
        TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath(referenceJsonFormat);
        String referenceJson = IOUtils.toString(TerraformModuleHelper.getDataFileReader(referenceJsonFormat));


        //then it extracts properly fields

        ImmutableTerraformModule expectedModule = ImmutableTerraformModule.builder()
                .moduleName("cloudflare-route-ondemandroute5")
                .source("modules/cloudflare")
                .putProperties("cloudflare_internet_domain", "${var.cloudflare_internet_domain}")
                .putProperties("cloudflare_root_domain", "${var.cloudflare_root_domain}")
                .putProperties("cloudflare_route_suffix", "${var.cloudflare_route_suffix}")
                .putProperties("org_guid", "${data.cloudfoundry_organization.org_on_demand_internet_route.id}")
                .putProperties("route-prefix", "ondemandroute5")
                .putProperties("service_instance_guid", "3456")
                .putProperties("space_guid", "${data.cloudfoundry_space.space_on_demand_internet_route.id}")
                .putOutputs(
                        "3456.started",
                        ImmutableOutputConfig.builder().value("${module.cloudflare-route-ondemandroute5.started}").build())
                .putOutputs(
                        "3456.completed",
                        ImmutableOutputConfig.builder().value("${module.cloudflare-route-ondemandroute5.completed}").build())
                .build();
        assertThat(deserialized).isEqualTo(expectedModule);


        //when it reserializes
        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
        String serialized = gson.toJson(deserialized);
        //System.err.println(serialized);
        assertThat(serialized).isEqualTo(referenceJson);

        TerraformModule parsed = TerraformModuleHelper.getGson().fromJson(serialized, ImmutableTerraformModule.class);

        //then it properly extracts fields
        assertEquals(parsed, deserialized);
        assertEquals(parsed, expectedModule);

    }

}
