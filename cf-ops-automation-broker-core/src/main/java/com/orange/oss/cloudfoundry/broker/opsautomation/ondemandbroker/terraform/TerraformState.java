package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.immutables.value.Value;

import java.util.Map;

/**
 * Represents a Terraform state file with its root module outputs, similar than what the terraform_remote_state enables loading
 * https://www.terraform.io/docs/providers/terraform/d/remote_state.html
 *
 */
@Value.Immutable
public abstract class TerraformState {

    public abstract Map<String, Output> getOutputs();

	@Value.Immutable
	public static abstract class Output {
		public abstract String getType();
		public abstract String getValue();
	}
}
