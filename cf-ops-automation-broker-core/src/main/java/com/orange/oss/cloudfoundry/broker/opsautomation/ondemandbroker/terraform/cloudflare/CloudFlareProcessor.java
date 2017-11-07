package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

/**
 *
 */
public class CloudFlareProcessor extends DefaultBrokerProcessor {
    private CloudFlareConfig cloudFlareConfig;
    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator;

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig) {
        this(cloudFlareConfig, new CloudFlareRouteSuffixValidator(cloudFlareConfig.getRouteSuffix()));

    }

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator) {
        this.cloudFlareConfig = cloudFlareConfig;
        this.cloudFlareRouteSuffixValidator = cloudFlareRouteSuffixValidator;
    }

    @Override
    public void preCreate(Context ctx) {
        //Fetch requested route and param name from Service Instance
        String paramName = "route";
        String arequestedroute = getRequestedRoute(ctx, paramName);
        validateRequestedRoute(arequestedroute, paramName);
    }

    String getRequestedRoute(Context ctx, String paramName) {
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
        return (String) request.getParameters().get(paramName);
    }

    public void validateRequestedRoute(String route, String paramName) {
        boolean valid = cloudFlareRouteSuffixValidator.isRouteValid(route);
        if (!valid) throw new RuntimeException("Invalid parameter " + paramName + " with value:" + route);
    }
}
