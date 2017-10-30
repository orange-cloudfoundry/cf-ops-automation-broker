package com.orange.oss.ondemandbroker;

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

    private ProcessorChain processorChain;

    public ProcessorChainServiceInstanceService(ProcessorChain processorChain) {
        this.processorChain = processorChain;
    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        processorChain.create();
        return new CreateServiceInstanceResponse();
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
        throw new UnsupportedOperationException("not yet implemented");
    }

}
