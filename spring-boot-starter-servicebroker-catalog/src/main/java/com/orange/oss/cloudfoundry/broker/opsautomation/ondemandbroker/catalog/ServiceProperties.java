package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cloud Foundry ServiceProperties
 * http://docs.cloudfoundry.org/services/api.html.
 *
 * Basically a duplicate of {@link org.springframework.cloud.servicebroker.model.ServiceDefinition ServiceDefinition }
 * with setters and validation.
 *
 * @author Sebastien Bortolussi
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ServiceProperties {

    public static final String NO_ID_ERROR = "Invalid configuration. No id has been set for service";
    public static final String NO_NAME_ERROR = "Invalid configuration. No name has been set for service";
    public static final String NO_DESCRIPTION_ERROR = "Invalid configuration. No description has been set for service";
    public static final String NO_PLAN_ERROR = "Invalid configuration. No plan has been set for service";

    /**
     * An identifier used to correlate this service in future requests to the catalog. This must be unique within
     * a Cloud Foundry deployment. Using a GUID is recommended.
     */
    @NotEmpty(message = NO_ID_ERROR)
    private String id;

    /**
     * A CLI-friendly name of the service that will appear in the catalog. The value should be all lowercase,
     * with no spaces.
     */
    @NotEmpty(message = NO_NAME_ERROR)
    private String name;

    /**
     * A user-friendly short description of the service that will appear in the catalog.
     */
    @NotEmpty(message = NO_DESCRIPTION_ERROR)
    private String description;

    /**
     * Indicates whether the service can be bound to applications.
     */
    private Boolean bindable = Boolean.TRUE;

    /**
     * Indicates whether the service supports requests to update instances to use a different plan from the one
     * used to provision a service instance.
     */
    private Boolean planUpdateable = Boolean.TRUE;

    /**
     * A list of plans for this service.
     */
    @Size(min=1,message = NO_PLAN_ERROR)
    @Valid
    private List<PlanProperties> plans = new ArrayList<>();

    /**
     * A list of tags to aid in categorizing and classifying services with similar characteristics.
     */
    private List<String> tags;

    /**
     * map of metadata to further describe a service offering.
     */
    private Map<String, Object> metadata;


    /**
     * A list of permissions that the user would have to give the service, if they provision it.
     */
    private List<String> requires;

    public ServiceProperties() {
    }

}
