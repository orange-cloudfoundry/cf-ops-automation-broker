package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CloudFlareProcessor extends DefaultBrokerProcessor {

    public static final String ROUTE_PREFIX = "route-prefix";
    private static Logger logger = LoggerFactory.getLogger(CloudFlareProcessor.class.getName());


    private CloudFlareConfig cloudFlareConfig;
    private CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator;
    private TerraformRepository.Factory repositoryFactory;
    private TerraformCompletionTracker completionTracker;

    public CloudFlareProcessor(CloudFlareConfig cloudFlareConfig, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator, TerraformRepository.Factory repositoryFactory, TerraformCompletionTracker completionTracker) {
        this.cloudFlareConfig = cloudFlareConfig;
        this.cloudFlareRouteSuffixValidator = cloudFlareRouteSuffixValidator;
        this.repositoryFactory = repositoryFactory;
        this.completionTracker = completionTracker;
    }

    @Override
    public void preBind(Context ctx) {
        throw new UnsupportedOperationException("Does not support bind requests");
    }

    @Override
    public void preCreate(Context ctx) {
        //Fetch requested routePrefix from Service Instance
        Map<String, Object> contextKeys = ctx.contextKeys;
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
        logger.debug("processing request " + request);

        //validate input params
        String routePrefix = (String) request.getParameters().get(ROUTE_PREFIX);
        validateRequestedRoute(routePrefix, ROUTE_PREFIX);


        ImmutableTerraformModule terraformModule = constructModule(request);
        TerraformRepository repository = getRepository(ctx); // lookup git clone for request, might throw runtime exception
        checkForConflictingModuleName(terraformModule, repository);
        checkForConflictingProperty(terraformModule, ROUTE_PREFIX, routePrefix, repository);

        repository.save(terraformModule);

        CreateServiceInstanceResponse response = new CreateServiceInstanceResponse();
        response.withAsync(true);
        response.withOperation(completionTracker.getCurrentDate());
        ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, response);
    }

    @Override
    public void preGetLastCreateOperation(Context ctx) {
        GetLastServiceOperationRequest operationRequest = (GetLastServiceOperationRequest)
                ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST);

        GetLastServiceOperationResponse operationResponse =
                completionTracker.getModuleExecStatus(getGitWorkDir(ctx), operationRequest.getServiceInstanceId(), operationRequest.getOperation());

        ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, operationResponse);
    }

    public ImmutableTerraformModule constructModule(CreateServiceInstanceRequest request) {
        //noinspection deprecation
        return ImmutableTerraformModule.builder()
                .from(cloudFlareConfig.getTemplate())
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", request.getOrganizationGuid())
                .putProperties(ROUTE_PREFIX, (String) request.getParameters().get(ROUTE_PREFIX))
                .putProperties("service_instance_guid", request.getServiceInstanceId())
                .putProperties("space_guid", request.getSpaceGuid())
                .outputs(new HashMap<>()) //clear sample outputs
                .putOutputs(
                        request.getServiceInstanceId() + ".started",
                        ImmutableOutputConfig.builder().value("${module." + request.getServiceInstanceId() + ".started}").build())
                .putOutputs(
                        request.getServiceInstanceId() + ".completed",
                        ImmutableOutputConfig.builder().value("${module." + request.getServiceInstanceId() + ".completed}").build())


                .build();
    }

    @Override
    public void preDelete(Context context) {
        DeleteServiceInstanceRequest request = (DeleteServiceInstanceRequest) context.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST);
        String serviceInstanceId = request.getServiceInstanceId();
        TerraformRepository repository = getRepository(context); //lookup git clone for request, might throw runtime exception
        TerraformModule terraformModule = repository.getByModuleName(serviceInstanceId);

        if (terraformModule == null) {
            logger.warn("Asked to delete a service instance with id=" + serviceInstanceId + " without any associated module in the repository");
        } else {
            repository.delete(terraformModule);
        }

        DeleteServiceInstanceResponse response = new DeleteServiceInstanceResponse();
        context.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE, response);
        String routePrefix = null;
        if (terraformModule != null) {
            routePrefix = terraformModule.getProperties().get(ROUTE_PREFIX);
        }
        String msg = getBrokerNameForCommitLog() + ": delete instance id=" + serviceInstanceId + " with " + ROUTE_PREFIX+"="+ routePrefix;
        context.contextKeys.put(GitProcessorContext.commitMessage.toString(), msg);
    }

    public String getBrokerNameForCommitLog() {
        return "cloudflare broker";
    }

    void validateRequestedRoute(String routePrefix, @SuppressWarnings("SameParameterValue") String paramName) {
        boolean valid = cloudFlareRouteSuffixValidator.isRouteValid(routePrefix);
        if (!valid) {
            logger.info("Invalid parameter " + paramName + " with value:" + routePrefix);
            throw new RuntimeException("Invalid parameter " + paramName + " with value:" + routePrefix);
        }
    }


    void checkForConflictingModuleName(TerraformModule requestedModule, TerraformRepository repository) {
        String requestedModuleModuleName = requestedModule.getModuleName();
        TerraformModule existing = repository.getByModuleName(requestedModuleModuleName);
        if (existing != null) {
            logger.warn("unexpected conflicting terraform module with name=" + requestedModuleModuleName + ". A module with same name already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("unexpected conflicting terraform module with name=" + requestedModuleModuleName + ". A module with same name already exists.");
        }
    }
    void checkForConflictingProperty(TerraformModule requestedModule, String propertyName, String userFacingParam, TerraformRepository repository) {
        String propertyValue = requestedModule.getProperties().get(propertyName);
        TerraformModule existing = repository.getByModuleProperty(propertyName, propertyValue);
        if (existing != null) {
            logger.info("received conflicting parameter " + userFacingParam + " with value:" + propertyValue  + " A module with same property "  + propertyName + " already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("Conflicting parameter " + userFacingParam + " with value:" + propertyValue  + " This value is already used by another service instance.");
        }
    }

    protected TerraformRepository getRepository(Context ctx) {
        Path gitWorkDir = getGitWorkDir(ctx);
        return repositoryFactory.getInstance(gitWorkDir);
    }

    protected Path getGitWorkDir(Context ctx) {
        Path path = (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
        if (path == null ) {
            logger.error("expected git repo clone dir to be available, but missing from context");
            throw new RuntimeException("missing git clone with dir for terraform config/state");
        }
        return path;
    }
}
