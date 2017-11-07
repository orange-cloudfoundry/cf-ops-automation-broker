package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

/**
 *
 */
public class TerraformModuleProcessor extends DefaultBrokerProcessor{

    public static final String ADD_TF_MODULE_WITH_ID = "AddTfModuleWithId";

    public TerraformModuleProcessor() {
    }

    @Override
    public void preCreate(Context ctx) {

    }

    public TerraformModule getRequestedTerraformModule(Context context) {
        CreateServiceInstanceRequest request= (CreateServiceInstanceRequest) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
        TerraformModule requestedTerraformModule = (TerraformModule) context.contextKeys.get(TerraformModuleProcessor.ADD_TF_MODULE_WITH_ID);

        return ImmutableTerraformModule.builder().from(requestedTerraformModule)
                .id(request.getServiceInstanceId()).build();

    }
}
