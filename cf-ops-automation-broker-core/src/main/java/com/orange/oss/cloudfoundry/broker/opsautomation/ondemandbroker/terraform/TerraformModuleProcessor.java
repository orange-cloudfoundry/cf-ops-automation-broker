package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

/**
 *
 */
public class TerraformModuleProcessor extends DefaultBrokerProcessor{

    private static Logger logger = LoggerFactory.getLogger(TerraformModuleProcessor.class.getName());


    public static final String ADD_TF_MODULE = "AddTfModuleWithId";
    private TerraformRepository repository;

    public TerraformModuleProcessor(TerraformRepository repository) {
        this.repository = repository;
    }

    @Override
    public void preCreate(Context ctx) {
        TerraformModule requestedTerraformModule = getRequestedTerraformModule(ctx);
        checkForConflictingModule(requestedTerraformModule);
        repository.save(requestedTerraformModule);
    }

    TerraformModule getRequestedTerraformModule(Context context) {
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
        TerraformModule requestedTerraformModule = (TerraformModule) context.contextKeys.get(TerraformModuleProcessor.ADD_TF_MODULE);

        return ImmutableTerraformModule.builder().from(requestedTerraformModule)
                .id(request.getServiceInstanceId()).build();

    }

    public void checkForConflictingModule(TerraformModule requestedModule) {
        String requestedModuleId = requestedModule.getId();
        TerraformModule existing = repository.getByModuleId(requestedModuleId);
        if (existing != null) {
            logger.warn("unexpected conflicting terraform module with id=" + requestedModuleId + ". A module with same id already exists:" + existing);
            //Don't return details on the existing module to the end user
            //as this may have confidential data
            throw new RuntimeException("unexpected conflicting terraform module with id=" + requestedModuleId + ". A module with same id already exists.");
        }
    }
}
