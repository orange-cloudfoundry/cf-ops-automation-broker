package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModule;
import org.immutables.value.Value;

/**
 *
 */
@Value.Immutable
public abstract class TerraformConfig {

    //FIXME: cloudflare specifics to be moved out
    public abstract String getRouteSuffix();

    @Value.Default
    public int getMaxExecutionDurationSeconds() {
        return 120;
    }

    public abstract TerraformModule getTemplate();
}
