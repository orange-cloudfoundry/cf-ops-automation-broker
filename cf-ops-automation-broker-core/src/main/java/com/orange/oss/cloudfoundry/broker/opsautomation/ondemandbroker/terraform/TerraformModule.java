package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Terraform module invocation from root module along with its outputs.
 * See https://www.terraform.io/docs/configuration/modules.html
 */
@Value.Immutable
public abstract class TerraformModule {

	/**
	 * The terraform module name is used as the module file name and needs to be unique and portable
	 * (i.e. not too long and without too special characters)
	 * Typically a service instance guid.
	 */
	public abstract String getModuleName();

	public abstract String getSource();


	public abstract Map<String, String> getProperties();

    @Value.Default
	public Map<String, OutputConfig> getOutputs() {
	    return new HashMap<>(); //optionally supports no outputs
    }


	/**
	 * Terraform output config. See https://www.terraform.io/docs/configuration/outputs.html
	 */
    @Value.Immutable
	public static abstract class OutputConfig {
		public abstract String getValue();
	}
}
