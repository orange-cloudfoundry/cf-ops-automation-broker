package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.ImmutableOutputConfig;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.ImmutableTerraformModule;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModuleProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.util.Map;

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
        Map<String, Object> contextKeys = ctx.contextKeys;
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);

        //validate input params
        String paramName = "route";
        String requestedRoute = (String) request.getParameters().get(paramName);
        validateRequestedRoute(requestedRoute, paramName);


        ImmutableTerraformModule terraformModule = ImmutableTerraformModule.builder()
                .from(cloudFlareConfig.getTemplate())
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", request.getOrganizationGuid())
                .putProperties("route-prefix", requestedRoute)
                .putProperties("service_instance_guid", request.getServiceInstanceId())
                .putProperties("space_guid", request.getSpaceGuid())
                .putOutputs(
                        request.getServiceInstanceId() + ".started",
                        ImmutableOutputConfig.builder().value("${module."+ request.getServiceInstanceId() + ".started}").build())
                .putOutputs(
                        request.getServiceInstanceId() + ".completed",
                        ImmutableOutputConfig.builder().value("${module." + request.getServiceInstanceId() + ".completed}").build())


                .build();

        contextKeys.put(TerraformModuleProcessor.ADD_TF_MODULE, terraformModule);

    }

    public void validateRequestedRoute(String route, String paramName) {
        boolean valid = cloudFlareRouteSuffixValidator.isRouteValid(route);
        if (!valid) throw new RuntimeException("Invalid parameter " + paramName + " with value:" + route);
    }
}
