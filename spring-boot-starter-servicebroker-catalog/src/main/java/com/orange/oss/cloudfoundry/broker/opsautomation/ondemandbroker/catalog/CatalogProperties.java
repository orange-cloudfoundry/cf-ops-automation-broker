package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Cloud Foundry Catalog
 * http://docs.cloudfoundry.org/services/api.html
 * Basically a duplicate of {@link org.springframework.cloud.servicebroker.model.Catalog Catalog}
 * with setters and validation.
 *
 * @author Sebastien Bortolussi
 */
@ConfigurationProperties(prefix = "servicebroker.catalog")
@Validated
@ToString
@Getter
@Setter
public class CatalogProperties {

    public static final String NO_SERVICE_ERROR = "Invalid configuration. No service has been defined";

    /**
     * A list of service offerings provided by the service broker.
     */
    @NotEmpty
    @Size(min = 1, message = NO_SERVICE_ERROR)
    @Valid
    private List<ServiceProperties> services;

    public CatalogProperties() {
    }

    public List<ServiceProperties> getServices() {
        return services;
    }


}
