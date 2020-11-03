package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
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


    public static final String GET_SERVICE_INSTANCE_REQUEST = "GetServiceInstanceRequest";
    public static final String GET_SERVICE_INSTANCE_RESPONSE = "GetServiceInstanceResponse";
    public static final String CREATE_SERVICE_INSTANCE_REQUEST = "CreateServiceInstanceRequest";
    public static final String CREATE_SERVICE_INSTANCE_RESPONSE = "CreateServiceInstanceResponse";
    public static final String GET_LAST_SERVICE_OPERATION_REQUEST = "GetLastServiceOperationRequest";
    public static final String GET_LAST_SERVICE_OPERATION_RESPONSE = "GetLastServiceOperationResponse";
    public static final String DELETE_SERVICE_INSTANCE_REQUEST = "DeleteServiceInstanceRequest";
    public static final String DELETE_SERVICE_INSTANCE_RESPONSE = "DeleteServiceInstanceResponse";
    @SuppressWarnings("WeakerAccess")
    public static final String UPDATE_SERVICE_INSTANCE_REQUEST = "UpdateServiceInstanceRequest";
    @SuppressWarnings("WeakerAccess")
    public static final String UPDATE_SERVICE_INSTANCE_RESPONSE = "UpdateServiceInstanceResponse";

    public static final String OSB_PROFILE_ORGANIZATION_GUID = "organization_guid";
    public static final String OSB_PROFILE_SPACE_GUID = "space_guid";




    private ProcessorChain processorChain;

    public ProcessorChainServiceInstanceService(ProcessorChain processorChain) {
        this.processorChain = processorChain;
    }

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
        try {
            Context ctx=new Context();
            ctx.contextKeys.put(CREATE_SERVICE_INSTANCE_REQUEST, request);
            processorChain.create(ctx);

            CreateServiceInstanceResponse response;
            if (ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_RESPONSE) instanceof CreateServiceInstanceResponse) {
                response = (CreateServiceInstanceResponse) ctx.contextKeys.get(CREATE_SERVICE_INSTANCE_RESPONSE);
            } else {
                response = CreateServiceInstanceResponse.builder().build();
            }
            return Mono.just(response);
        } catch (RuntimeException e) {
            logger.info("Unable to create service with request " + request + ", caught " + e, e);
            throw ProcessorChainServiceHelper.processInternalException(e);
        }
    }

    @Override
    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
        try {
            Context ctx=new Context();
            ctx.contextKeys.put(GET_SERVICE_INSTANCE_REQUEST, request);
            processorChain.getInstance(ctx);

            GetServiceInstanceResponse response;
            if (ctx.contextKeys.get(GET_SERVICE_INSTANCE_RESPONSE) instanceof GetServiceInstanceResponse) {
                response = (GetServiceInstanceResponse) ctx.contextKeys.get(GET_SERVICE_INSTANCE_RESPONSE);
            } else {
                response = GetServiceInstanceResponse.builder().build();
            }
            return Mono.just(response);
        } catch (RuntimeException e) {
            logger.info("Unable to fetch service with request " + request + ", caught " + e, e);
            throw ProcessorChainServiceHelper.processInternalException(e);
        }
    }

    @Override
    public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
        try {
            Context ctx = new Context();
            ctx.contextKeys.put(UPDATE_SERVICE_INSTANCE_REQUEST, request);

            processorChain.update(ctx);
            UpdateServiceInstanceResponse response;
            if (ctx.contextKeys.get(UPDATE_SERVICE_INSTANCE_RESPONSE) instanceof UpdateServiceInstanceResponse) {
                response = (UpdateServiceInstanceResponse) ctx.contextKeys.get(UPDATE_SERVICE_INSTANCE_RESPONSE);
            } else {
                response = UpdateServiceInstanceResponse.builder().build();
            }
            return Mono.just(response);
        } catch (RuntimeException e) {
            logger.info("Unable to update service with request " + request + ", caught " + e, e);
            throw ProcessorChainServiceHelper.processInternalException(e);
        }
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
        try {
            Context ctx = new Context();
            ctx.contextKeys.put(DELETE_SERVICE_INSTANCE_REQUEST, request);

            processorChain.delete(ctx);
            DeleteServiceInstanceResponse response;
            if (ctx.contextKeys.get(DELETE_SERVICE_INSTANCE_RESPONSE) instanceof DeleteServiceInstanceResponse) {
                response = (DeleteServiceInstanceResponse) ctx.contextKeys.get(DELETE_SERVICE_INSTANCE_RESPONSE);
            } else {
                response = DeleteServiceInstanceResponse.builder().build();
            }
            return Mono.just(response);
        } catch (RuntimeException e) {
            logger.info("Unable to delete service with request " + request + ", caught " + e, e);
            throw ProcessorChainServiceHelper.processInternalException(e);
        }
    }

    @Override
    public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
        try {
            Context ctx = new Context();
            ctx.contextKeys.put(GET_LAST_SERVICE_OPERATION_REQUEST, request);
            processorChain.getLastOperation(ctx);

            GetLastServiceOperationResponse response;
            if (ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE) instanceof GetLastServiceOperationResponse) {
                response = (GetLastServiceOperationResponse) ctx.contextKeys.get(GET_LAST_SERVICE_OPERATION_RESPONSE);
            } else {
                response = GetLastServiceOperationResponse.builder().operationState(OperationState.SUCCEEDED).build();
            }
            return Mono.just(response);
        } catch (RuntimeException e) {
            logger.info("Unable to getLastOperation with request " + request + ", caught " + e, e);
            throw ProcessorChainServiceHelper.processInternalException(e);
        }
    }

}
