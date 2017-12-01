package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.UserFacingRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static Logger logger = LoggerFactory.getLogger(ProcessorChainServiceInstanceService.class.getName());


    public static final String CREATE_SERVICE_INSTANCE_REQUEST = "CreateServiceInstanceRequest";
    public static final String CREATE_SERVICE_INSTANCE_RESPONSE = "CreateServiceInstanceResponse";
    public static final String GET_LAST_SERVICE_OPERATION_REQUEST = "GetLastServiceOperationRequest";
    public static final String GET_LAST_SERVICE_OPERATION_RESPONSE = "GetLastServiceOperationResponse";
    public static final String DELETE_SERVICE_INSTANCE_REQUEST = "DeleteServiceInstanceRequest";
    public static final String DELETE_SERVICE_INSTANCE_RESPONSE = "DeleteServiceInstanceResponse";

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
        Context ctx = new Context();
        ctx.contextKeys.put(DELETE_SERVICE_INSTANCE_REQUEST, request);

        processorChain.delete(ctx);
        DeleteServiceInstanceResponse response;
        if (ctx.contextKeys.get(DELETE_SERVICE_INSTANCE_RESPONSE) instanceof DeleteServiceInstanceResponse) {
            response = (DeleteServiceInstanceResponse) ctx.contextKeys.get(DELETE_SERVICE_INSTANCE_RESPONSE);
        } else {
            response = new DeleteServiceInstanceResponse();
        }
        return response;
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
        Context ctx = new Context();
        ctx.contextKeys.put(GET_LAST_SERVICE_OPERATION_REQUEST, request);
        processorChain.getLastOperation(ctx);

        GetLastServiceOperationResponse response;
        if (ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE) instanceof GetLastServiceOperationResponse) {
            response = (GetLastServiceOperationResponse) ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE);
        } else {
            response = new GetLastServiceOperationResponse();
        }
        return response;
    }

    public RuntimeException filterInternalException(RuntimeException exception) {
        if (exception instanceof UserFacingRuntimeException) {
            return exception;
        } else {
            return new RuntimeException(); //filter out potential confidential data
        }
    }

}
