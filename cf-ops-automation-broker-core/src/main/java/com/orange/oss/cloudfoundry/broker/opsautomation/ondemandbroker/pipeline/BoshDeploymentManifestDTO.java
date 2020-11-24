package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supports parsing Bosh manifest file to extract the nested "coab_completion_marker" field.
 * All other fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoshDeploymentManifestDTO {

	/**
	 * Bosh deployment name to assign in the manifest by operators file
	 */
	@JsonProperty("coab_completion_marker")
	@JsonInclude(value= JsonInclude.Include.NON_NULL)
	public CoabVarsFileDto coabCompletionMarker;

}
