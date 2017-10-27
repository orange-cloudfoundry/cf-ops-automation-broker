package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class CloudFlareProcessorTest {


    @Test
    public void requires_config() {
        String routeSuffix = "-cdn-cw-vdr-pprod-apps.elpaaso.net";
        CloudFlareConfig cloudFlareConfig = new CloudFlareConfig(routeSuffix);
        assertThat(cloudFlareConfig.getRouteSuffix()).isEqualTo(routeSuffix);

        CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig);
    }

    @Test(expected = RuntimeException.class)
    public void validates_requested_route_validity() {
        CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(aConfig());

        //Given an invalid route
        cloudFlareProcessor.validateRequestedRoute("", "route");
    }

    @Test
    public void rejects_duplicate_route_request() {

    }

    private CloudFlareConfig aConfig() {
        String routeSuffix = "-cdn-cw-vdr-pprod-apps.elpaaso.net";
        CloudFlareConfig cloudFlareConfig = new CloudFlareConfig(routeSuffix);

        return cloudFlareConfig;
    }


}
