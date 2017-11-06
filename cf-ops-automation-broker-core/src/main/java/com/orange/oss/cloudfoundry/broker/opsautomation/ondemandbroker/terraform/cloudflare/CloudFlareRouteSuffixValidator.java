package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.apache.commons.validator.routines.DomainValidator;

/**
 *
 */
public class CloudFlareRouteSuffixValidator {
    private String suffix;

    public CloudFlareRouteSuffixValidator(String suffix) {
        this.suffix = suffix;
    }

    public boolean isRouteValid(String routePrefix) {
        if (routePrefix == null || routePrefix.isEmpty()) {
            return false;
        }
        //Guava might be too heavy
        //InternetDomain internetDomain = InternetDomain.from(routePrefix+suffix);

        DomainValidator domainValidator = DomainValidator.getInstance();
        return domainValidator.isValid(routePrefix+suffix);
    }
}
