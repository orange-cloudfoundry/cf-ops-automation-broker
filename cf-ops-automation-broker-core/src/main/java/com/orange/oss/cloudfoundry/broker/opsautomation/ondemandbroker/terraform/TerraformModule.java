package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Terraform module invocation.
 */
@Value.Immutable
public abstract class TerraformModule {

	public abstract String getModuleName();

	public abstract String getSource();

	/**
	 * The Id is used as the module file name and needs to be unique and portable
	 * (i.e. not too long and without too special characters)
	 * Typically a service instance guid.
	 */
	@Value.Default
	public String getId() {
		return "0"; //a default value to allow factorization of assigning Id into TerraformModuleProcessor
	}

	public abstract Map<String, String> getProperties();
}
