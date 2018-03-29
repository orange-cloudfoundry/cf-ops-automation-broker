package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import static com.orange.oss.ondemandbroker.ProcessorChainServiceHelper.processInternalException;

/**
 * Delegates to a {@link ProcessorChain} on requests to create and delete service instance bindings.
 *
 * @author Sebastien Bortolussi
 */
@Service
public class ProcessorChainServiceInstanceBindingService implements ServiceInstanceBindingService {
    private static Logger logger = LoggerFactory.getLogger(ProcessorChainServiceInstanceService.class.getName());

    @SuppressWarnings("WeakerAccess")
    public static final String CREATE_SERVICE_INSTANCE_BINDING_REQUEST = "CreateServiceInstanceBindingRequest";
    @SuppressWarnings("WeakerAccess")
    public static final String CREATE_SERVICE_INSTANCE_BINDING_RESPONSE = "CreateServiceInstanceBindingResponse";
    @SuppressWarnings("WeakerAccess")
    public static final String DELETE_SERVICE_INSTANCE_BINDING_REQUEST = "DeleteServiceInstanceBindingRequest";


    private ProcessorChain processorChain;

    public ProcessorChainServiceInstanceBindingService(ProcessorChain processorChain) {
        this.processorChain = processorChain;
    }

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        try {
            Context ctx= new Context();
            ctx.contextKeys.put(CREATE_SERVICE_INSTANCE_BINDING_REQUEST, request);
            processorChain.bind(ctx);

            CreateServiceInstanceBindingResponse response;
            if (ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_BINDING_RESPONSE) instanceof CreateServiceInstanceBindingResponse) {
                response = (CreateServiceInstanceBindingResponse) ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_BINDING_RESPONSE);
            } else {
                response = new CreateServiceInstanceBindingResponse();
            }
            return response;
        } catch (RuntimeException e) {
            logger.info("Unable to create service binding with request " + request + ", caught " + e, e);
            throw processInternalException(e);
        }

    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        try {
            Context ctx= new Context();
            ctx.contextKeys.put(DELETE_SERVICE_INSTANCE_BINDING_REQUEST, request);
            processorChain.unBind(ctx);
        } catch (RuntimeException e) {
            logger.info("Unable to delete service binding with request " + request + ", caught " + e, e);
            throw processInternalException(e);
        }
    }


}
