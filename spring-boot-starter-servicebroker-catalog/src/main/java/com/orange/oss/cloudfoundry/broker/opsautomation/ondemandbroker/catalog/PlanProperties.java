package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Foundry plan
 * see http://docs.cloudfoundry.org/services/catalog-metadata.html#plan-metadata-fields
 *
 * Basically a duplicate of {@link org.springframework.cloud.servicebroker.model.Plan Plan}
 * with setters and validation.
 *
 * @author Sebastien Bortolussi
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class PlanProperties {

    public static final String NO_ID_ERROR = "Invalid configuration. No id has been set for plan";
    public static final String NO_DESCRIPTION_ERROR = "Invalid configuration. No description has been set for service";

    public static final String PLAN_NAME_DEFAULT = "default";

    /**
     * An identifier used to correlate this plan in future requests to the catalog. This must be unique within
     * a Cloud Foundry deployment. Using a GUID is recommended.
     */
    @NotEmpty(message = NO_ID_ERROR)
    private String id;

    /**
     * A CLI-friendly name of the plan that will appear in the catalog. The value should be all lowercase,
     * with no spaces.
     */
    @NotEmpty
    private String name = PLAN_NAME_DEFAULT;

    /**
     * A user-friendly short description of the plan that will appear in the catalog.
     */
    @NotEmpty(message = NO_DESCRIPTION_ERROR)
    private String description;

    /**
     * map of metadata to further describe a service plan.
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Indicates whether the service with this plan can be bound to applications. True by default.
     */
    private Boolean bindable;

    /**
     * Indicates whether the plan can be limited by the non_basic_services_allowed field in a Cloud Foundry Quota.
     */
    private Boolean free = Boolean.TRUE;

    public PlanProperties() {
    }

    public void setMetadata(String metadataJson) {
        Type type = new TypeToken<HashMap<String, Object>>() {
        }.getType();
        this.metadata = new Gson().fromJson(metadataJson, type);
    }
}
