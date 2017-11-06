package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;

/**
 *
 */
public class CloudFlareProcessor extends DefaultBrokerProcessor {
    private CloudFlareConfig cloudFlareConfig;
    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator;

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig) {

        this.cloudFlareConfig = cloudFlareConfig;
        cloudFlareRouteSuffixValidator = new CloudFlareRouteSuffixValidator(cloudFlareConfig.getRouteSuffix());

    }

    @Override
    public void preCreate(Context ctx) {
        //Fetch requested route and param name from Service Instance
        validateRequestedRoute("arequestedroute", "route");
    }

    public void validateRequestedRoute(String route, String paramName) {
        boolean valid = cloudFlareRouteSuffixValidator.isRouteValid(route);
        if (!valid) throw new RuntimeException("Invalid parameter " + paramName + " with value:" + route);
    }
}
