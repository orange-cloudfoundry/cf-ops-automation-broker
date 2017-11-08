package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

/**
 * Delegates to a {@link ProcessorChain} on requests to create and delete service instances.
 *
 * @author Sebastien Bortolussi
 */
@Service
public class ProcessorChainServiceInstanceService implements ServiceInstanceService {

    public static final String CREATE_SERVICE_INSTANCE_REQUEST = "CreateServiceInstanceRequest";
    public static final String CREATE_SERVICE_INSTANCE_RESPONSE = "CreateServiceInstanceResponse";
    public static final String GET_LAST_SERVICE_OPERATION_REQUEST = "GetLastServiceOperationRequest";
    public static final String GET_LAST_SERVICE_OPERATION_RESPONSE = "GetLastServiceOperationResponse";

    private ProcessorChain processorChain;

    public ProcessorChainServiceInstanceService(ProcessorChain processorChain) {
        this.processorChain = processorChain;
    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        Context ctx=new Context();
        ctx.contextKeys.put(CREATE_SERVICE_INSTANCE_REQUEST, request);
        processorChain.create(ctx);

        CreateServiceInstanceResponse response;
        if (ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_RESPONSE) instanceof CreateServiceInstanceResponse) {
            response = (CreateServiceInstanceResponse) ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_RESPONSE);
        } else {
            response = new CreateServiceInstanceResponse();
        }
        return response;
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
        processorChain.delete();
        return new DeleteServiceInstanceResponse();
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
        Context ctx = new Context();
        ctx.contextKeys.put(GET_LAST_SERVICE_OPERATION_REQUEST, request);
        processorChain.getLastCreateOperation(ctx);

        GetLastServiceOperationResponse response;
        if (ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE) instanceof GetLastServiceOperationResponse) {
            response = (GetLastServiceOperationResponse) ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE);
        } else {
            response = new GetLastServiceOperationResponse();
        }
        return response;
    }

}
