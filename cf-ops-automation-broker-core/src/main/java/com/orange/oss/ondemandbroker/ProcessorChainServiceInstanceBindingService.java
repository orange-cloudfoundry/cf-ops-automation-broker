package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

/**
 * Delegates to a {@link ProcessorChain} on requests to create and delete service instance bindings.
 *
 * @author Sebastien Bortolussi
 */
@Service
public class ProcessorChainServiceInstanceBindingService implements ServiceInstanceBindingService {

    private ProcessorChain processorChain;

    public ProcessorChainServiceInstanceBindingService(ProcessorChain processorChain) {
        this.processorChain = processorChain;
    }

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        processorChain.bind();
        return new CreateServiceInstanceBindingResponse();
    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest response) {
        processorChain.unBind();
    }

}
