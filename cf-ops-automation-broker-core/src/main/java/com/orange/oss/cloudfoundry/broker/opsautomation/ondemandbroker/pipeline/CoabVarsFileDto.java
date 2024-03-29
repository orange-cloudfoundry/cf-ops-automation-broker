package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;

import static java.util.Arrays.asList;

/**
 * Formats vars files with user inputs to COA deployment models
 */
public class CoabVarsFileDto {

    /**
     * \d 	A digit: [0-9]
     * \w 	A word character: [a-zA-Z_0-9]
     */
    public static final String WHITE_LISTED_PATTERN = "[\\w\\d _.-]*";
    public static final String WHITE_LISTED_MESSAGE = "must only contain a-Z, 0-9, space, _, dot, and - chars";

    /**
     * Name of the maps key (and subkeys) which should be applied relaxed patterns
     * This is designed to support JSON Serialized fields that COA models ignore
     */
    public static final List<String> RELAXED_KEY_NAMES= asList(
        "brokered_service_api_info_location", //contain forward slashes as in api.redacted-domain.org/v2/info
        //K8S service catalog annotations which contain JSON serialized chars that we accept
        "brokered_service_originating_identity_username",
        "brokered_service_originating_identity_groups",
        //CF annotations sent as Json serialized
        "brokered_service_context_organization_annotations",
        "brokered_service_context_space_annotations",
        "brokered_service_context_instance_annotations");
    /**
     * \d 	A digit: [0-9]
     * \w 	A word character: [a-zA-Z_0-9]
     */
    public static final String RELAXED_WHITE_LISTED_PATTERN = "[\\w\\d _.,/\\-:\\[\\]\"{}=]*";
    public static final String RELAXED_WHITE_LISTED_MESSAGE = "must only contain a-Z, 0-9, space, _ (underscore), . (dot), , " +
        "(comma), : (column), left and right brackets ([]), braces ({}), equal (=), quote (\"), slash (/) and dash " +
        "(-) chars";


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
     * Maintenance_info attached to the Service Instance (at provision or update time)
     */
    @JsonProperty("maintenance_info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public MaintenanceInfo maintenanceInfo;

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

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class PreviousValues {
        /**
         * The ID of the plan prior to the update, from OSB
         * https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#previous-values-object
         */
        @JsonProperty("plan_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Pattern(regexp = WHITE_LISTED_PATTERN, message = WHITE_LISTED_MESSAGE)
        public String plan_id;

        @JsonProperty("maintenance_info")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public MaintenanceInfo maintenanceInfo;

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
