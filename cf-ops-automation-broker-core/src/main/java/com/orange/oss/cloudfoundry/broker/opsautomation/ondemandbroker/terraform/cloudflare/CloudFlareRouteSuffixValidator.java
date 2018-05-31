package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.apache.commons.validator.routines.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CloudFlareRouteSuffixValidator {
    private String suffix;
    private DomainValidator domainValidator;

    private static Logger logger = LoggerFactory.getLogger(TerraformProcessor.class.getName());


    public CloudFlareRouteSuffixValidator(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            throw new RuntimeException("expected non-empty suffix");
        }
        this.suffix = suffix;
        this.domainValidator = DomainValidator.getInstance();
    }

    public boolean isRouteValid(String routePrefix) {
        if (routePrefix == null || routePrefix.isEmpty() || routePrefix.contains(".") ) {
            return false;
        }
        //Guava might be too heavy
        //InternetDomain internetDomain = InternetDomain.from(routePrefix+suffix);

        boolean valid = domainValidator.isValid(routePrefix + suffix);
        if (!valid) {
            logger.info("Invalid domain : " + routePrefix + suffix);
        }
        return valid;
    }
}
