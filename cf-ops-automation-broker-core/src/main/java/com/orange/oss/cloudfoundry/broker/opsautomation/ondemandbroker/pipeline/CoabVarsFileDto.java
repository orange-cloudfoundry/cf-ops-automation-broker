package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Formats vars files with user inputs to COA deployment models
 */
public class CoabVarsFileDto {

    /**
     * \d 	A digit: [0-9]
     * \w 	A word character: [a-zA-Z_0-9]
     */
    public static final String WHITE_LISTED_PATTERN = "[\\w\\d _./-]*";
    public static final String WHITE_LISTED_MESSAGE = "must only contain a-Z, 0-9, space, _, dot, slash and - chars";
    /**
     * Bosh deployment name to assign in the manifest by operators file
     */
    @JsonProperty("deployment_name")
    @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
    @NotEmpty
    public String deployment_name;

    /**
     * :instance_id MUST be a globally unique non-empty string. This ID will be used for future requests (bind and deprovision), so the Service Broker will use it to correlate the resource it creates.
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#route-2
     */
    @JsonProperty("instance_id")
    @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
    @NotEmpty
    public String instance_id;

    /**
     * ID of a service from the catalog for this Service Broker from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("service_id")
    @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
    @NotEmpty
    public String service_id;

    /**
     * ID of a plan from the service that has been requested from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("plan_id")
    @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
    @NotEmpty
    public String plan_id;

    /**
     * Platform specific contextual information under which the Service Instance is to be provisioned, from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("context")
    @Valid
    public final CloudFoundryOsbContext context = new CloudFoundryOsbContext();

    /**
     * Configuration parameters for the Service Instance, from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("parameters")
    @JsonInclude(JsonInclude.Include.NON_NULL) //include even if empty
    public final Map<String, Object> parameters = new HashMap<>();

    /**
     * For update requests,Information about the Service Instance prior to the updatefrom
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("previous_values")
    @Valid
    public PreviousValues previous_values;

    public static class CloudFoundryOsbContext {
        @JsonProperty("platform")
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String platform = "cloudfoundry";
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("user_guid")
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String user_guid;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("space_guid")
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String space_guid;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("organization_guid")
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String organization_guid;
    }

    public static class PreviousValues {
        /**
         * The ID of the plan prior to the update, from OSB
         * https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#previous-values-object
         */
        @NotEmpty
        @JsonProperty("plan_id")
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String plan_id;
    }
}
