package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;

import java.util.Map;

/**
 *
 */
public class CloudFlareProcessor extends DefaultBrokerProcessor {

    private static final String ROUTE_PREFIX = "route-prefix";
    private static Logger logger = LoggerFactory.getLogger(CloudFlareProcessor.class.getName());


    private CloudFlareConfig cloudFlareConfig;
    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator;
    private TerraformRepository repository;
    private TerraformCompletionTracker completionTracker;

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, TerraformRepository repository) {
        this(cloudFlareConfig, new CloudFlareRouteSuffixValidator(cloudFlareConfig.getRouteSuffix()), repository, null);

    }

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator, TerraformRepository repository, TerraformCompletionTracker completionTracker) {
        this.cloudFlareConfig = cloudFlareConfig;
        this.cloudFlareRouteSuffixValidator = cloudFlareRouteSuffixValidator;
        this.repository = repository;
        this.completionTracker = completionTracker;
    }

    @Override
    public void preCreate(Context ctx) {
        //Fetch requested route and param name from Service Instance
        Map<String, Object> contextKeys = ctx.contextKeys;
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);

        //validate input params
        String route = (String) request.getParameters().get("route");
        validateRequestedRoute(route, "route");


        ImmutableTerraformModule terraformModule = constructModule(request);

        checkForConflictingModuleId(terraformModule);
        checkForConflictingModuleName(terraformModule);
        checkForConflictingProperty(terraformModule, ROUTE_PREFIX, route);

        repository.save(terraformModule);

        CreateServiceInstanceResponse response = new CreateServiceInstanceResponse();
        response.withAsync(true);
        response.withOperation(completionTracker.getCurrentDate());
        ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, response);
    }

    @Override
    public void preGetLastCreateOperation(Context ctx) {
        GetLastServiceOperationRequest operationRequest = (GetLastServiceOperationRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST);

        GetLastServiceOperationResponse operationResponse = completionTracker.getModuleExecStatus(operationRequest.getServiceInstanceId(), operationRequest.getOperation());

        ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, operationResponse);
    }

    public ImmutableTerraformModule constructModule(CreateServiceInstanceRequest request) {
        return ImmutableTerraformModule.builder()
                .from(cloudFlareConfig.getTemplate())
                .id(request.getServiceInstanceId())
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", request.getOrganizationGuid())
                .putProperties(ROUTE_PREFIX, (String) request.getParameters().get("route"))
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
    public void checkForConflictingProperty(TerraformModule requestedModule, String propertyName, String userFacingParam) {
        String propertyValue = requestedModule.getProperties().get(propertyName);
        TerraformModule existing = repository.getByModuleProperty(propertyName, propertyValue);
        if (existing != null) {
            logger.info("received conflicting parameter " + userFacingParam + " with value:" + propertyValue  + " A module with same property "  + propertyName + " already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("Conflicting parameter " + userFacingParam + " with value:" + propertyValue  + " This value is already used by another service instance.");
        }
    }

}
