package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModule;
import org.immutables.value.Value;

/**
 *
 */
@Value.Immutable
public abstract class CloudFlareConfig {

    public abstract String getRouteSuffix();

    public abstract TerraformModule getTemplate();
}
