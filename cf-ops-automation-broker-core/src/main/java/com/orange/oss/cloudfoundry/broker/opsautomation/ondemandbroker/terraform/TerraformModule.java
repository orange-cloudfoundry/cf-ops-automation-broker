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

	public abstract Map<String, String> getProperties();
}
