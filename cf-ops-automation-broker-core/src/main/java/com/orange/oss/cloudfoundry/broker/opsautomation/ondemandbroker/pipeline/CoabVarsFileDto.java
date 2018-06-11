package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.HashMap;

/**
 * Formats vars files with user inputs to COA deployment models
 */
public class CoabVarsFileDto {

    /**
     * Bosh deployment name to assign in the manifest by operators file
     */
    @JsonProperty("deployment_name")
    public String deployment_name;

    /**
     * ID of a service from the catalog for this Service Broker from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("service_id")
    public String service_id;
    /**
     * ID of a plan from the service that has been requested from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("plan_id")
    public String plan_id;

    /**
     * Platform specific contextual information under which the Service Instance is to be provisioned, from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("context")
    public final CloudFoundryOsbContext context = new CloudFoundryOsbContext();

    /**
     * Configuration parameters for the Service Instance, from
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonProperty("parameters")
    @JsonInclude(JsonInclude.Include.NON_NULL) //include even if empty
    public final HashMap<String, Object> parameters = new HashMap<>();

    /**
     * For update requests,Information about the Service Instance prior to the updatefrom
     * OSB https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#body-2
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("previous_values")
    public PreviousValues previous_values;

    public static class CloudFoundryOsbContext {
        @JsonProperty("platform")
        public String platform = "cloudfoundry";
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("user_guid")
        public String user_guid;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("space_guid")
        public String space_guid;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("organization_guid")
        public String organization_guid;
    }

    public static class PreviousValues {
        /**
         * The ID of the plan prior to the update, from OSB
         * https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#previous-values-object
         */
        @NotEmpty
        @JsonProperty("plan_id")
        public String plan_id;
    }
}
