package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

/**
 * Represents a Terraform module invocation.
 */
@Value.Immutable
public abstract class TerraformState {

	public abstract List<Module> getModules();

	@Value.Immutable
	public static abstract class Module {

		public abstract String getModulePath();

		public abstract Map<String, Output> getOutputs();
	}


	@Value.Immutable
	public static abstract class Output {
		public abstract String getType();
		public abstract String getValue();

	}
}
