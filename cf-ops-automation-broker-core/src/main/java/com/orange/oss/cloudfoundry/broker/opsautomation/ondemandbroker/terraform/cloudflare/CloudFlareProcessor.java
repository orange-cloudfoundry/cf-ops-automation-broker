package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.util.Map;

/**
 *
 */
public class CloudFlareProcessor extends DefaultBrokerProcessor {

    private static Logger logger = LoggerFactory.getLogger(CloudFlareProcessor.class.getName());


    private CloudFlareConfig cloudFlareConfig;
    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator;
    private TerraformRepository repository;

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, TerraformRepository repository) {
        this(cloudFlareConfig, new CloudFlareRouteSuffixValidator(cloudFlareConfig.getRouteSuffix()), repository);

    }

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator, TerraformRepository repository) {
        this.cloudFlareConfig = cloudFlareConfig;
        this.cloudFlareRouteSuffixValidator = cloudFlareRouteSuffixValidator;
        this.repository = repository;
    }

    @Override
    public void preCreate(Context ctx) {
        //Fetch requested route and param name from Service Instance
        Map<String, Object> contextKeys = ctx.contextKeys;
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);

        //validate input params
        validateRequestedRoute((String) request.getParameters().get("route"), "route");


        ImmutableTerraformModule terraformModule = constructModule(request);

        checkForConflictingModuleId(terraformModule);
        checkForConflictingModuleName(terraformModule);

        repository.save(terraformModule);
    }

    public ImmutableTerraformModule constructModule(CreateServiceInstanceRequest request) {
        return ImmutableTerraformModule.builder()
                .from(cloudFlareConfig.getTemplate())
                .id(request.getServiceInstanceId())
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", request.getOrganizationGuid())
                .putProperties("route-prefix", (String) request.getParameters().get("route"))
                .putProperties("service_instance_guid", request.getServiceInstanceId())
                .putProperties("space_guid", request.getSpaceGuid())
                .putOutputs(
                        request.getServiceInstanceId() + ".started",
                        ImmutableOutputConfig.builder().value("${module." + request.getServiceInstanceId() + ".started}").build())
                .putOutputs(
                        request.getServiceInstanceId() + ".completed",
                        ImmutableOutputConfig.builder().value("${module." + request.getServiceInstanceId() + ".completed}").build())


                .build();
    }

    public void validateRequestedRoute(String route, @SuppressWarnings("SameParameterValue") String paramName) {
        boolean valid = cloudFlareRouteSuffixValidator.isRouteValid(route);
        if (!valid) throw new RuntimeException("Invalid parameter " + paramName + " with value:" + route);
    }


    public void checkForConflictingModuleId(TerraformModule requestedModule) {
        String requestedModuleId = requestedModule.getId();
        TerraformModule existing = repository.getByModuleId(requestedModuleId);
        if (existing != null) {
            logger.warn("unexpected conflicting terraform module with id=" + requestedModuleId + ". A module with same id already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("unexpected conflicting terraform module with id=" + requestedModuleId + ". A module with same id already exists.");
        }
    }

    public void checkForConflictingModuleName(TerraformModule requestedModule) {
        String requestedModuleModuleName = requestedModule.getModuleName();
        TerraformModule existing = repository.getByModuleName(requestedModuleModuleName);
        if (existing != null) {
            logger.warn("unexpected conflicting terraform module with name=" + requestedModuleModuleName + ". A module with same name already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("unexpected conflicting terraform module with name=" + requestedModuleModuleName + ". A module with same name already exists.");
        }
    }

}
