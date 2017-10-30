package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceBindingService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceBindingServiceTest {

    @Mock
    ProcessorChain processorChain;

    @InjectMocks
    ProcessorChainServiceInstanceBindingService service;

    @Test
    public void should_chain_bind_processors_on_service_instance_binding() throws Exception {
        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest();

        CreateServiceInstanceBindingResponse response = service.createServiceInstanceBinding(request);

        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceBindingResponse());
        Mockito.verify(processorChain).bind();

    }

    @Test
    public void should_chain_unbind_processors_on_service_instance_unbinding() throws Exception {
        DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest("instance_id",
                "binding_id",
                "service_definition_id",
                "plan_id",
                new ServiceDefinition());

        service.deleteServiceInstanceBinding(request);

        Mockito.verify(processorChain).unBind();
    }

}