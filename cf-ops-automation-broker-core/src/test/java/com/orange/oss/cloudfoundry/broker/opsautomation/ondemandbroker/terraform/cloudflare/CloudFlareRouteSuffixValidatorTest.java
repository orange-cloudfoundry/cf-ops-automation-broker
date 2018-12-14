package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class CloudFlareRouteSuffixValidatorTest {

    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator = new CloudFlareRouteSuffixValidator("-cdn-cw-vdr-pprod-apps.redacted-domain.org");

    @Test(expected = RuntimeException.class)
    public void rejects_empty_suffix() {
        new CloudFlareRouteSuffixValidator("");
    }
    @Test(expected = RuntimeException.class)
    public void rejects_null_suffix() {
        new CloudFlareRouteSuffixValidator(null);
    }

    @Test
    public void accepts_valid_routes() {
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("route")).isTrue();
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("route1234")).isTrue();
    }

    @Test
    public void rejects_empty_routes() {
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("")).isFalse();
    }

    @Test
    public void rejects_too_long_routes() throws Exception {
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-too-long-route")).isFalse();
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("tootootootootootootootootootootootootootootootootootootootootootootootootootootootoo-long-route")).isFalse();

    }

    @Test
    public void rejects_injection_attacks() {
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("\"route")).isFalse();
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("\'route")).isFalse();

    }

    @Test
    public void rejects_nested_subdomains() {
        Assertions.assertThat(cloudFlareRouteSuffixValidator.isRouteValid("route.subdomain")).isFalse();
    }

}