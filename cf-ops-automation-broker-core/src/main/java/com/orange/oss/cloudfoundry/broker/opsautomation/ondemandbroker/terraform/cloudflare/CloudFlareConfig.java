package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModule;

/**
 *
 */
public class CloudFlareConfig {
    private String routeSuffix;

    public CloudFlareConfig(String routeSuffix, TerraformModule template) {
        this.routeSuffix = routeSuffix;
    }

    public String getRouteSuffix() {
        return routeSuffix;
    }
}
