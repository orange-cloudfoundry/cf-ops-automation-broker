package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

/**
 *
 */
public class CloudFlareConfig {
    private String routeSuffix;

    public CloudFlareConfig(String routeSuffix) {
        this.routeSuffix = routeSuffix;
    }

    public String getRouteSuffix() {
        return routeSuffix;
    }
}
