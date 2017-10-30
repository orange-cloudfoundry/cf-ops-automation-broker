package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceServiceTest {

    @Mock
    ProcessorChain processorChain;

    @InjectMocks
    ProcessorChainServiceInstanceService processorChainServiceInstanceService;

    @Test
    public void should_chain_create_processors_on_service_instance_creation() throws Exception {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id");

        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceResponse());
        Mockito.verify(processorChain).create();
    }

    @Test
    public void should_chain_delete_processors_on_service_instance_deletion() throws Exception {
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);

        DeleteServiceInstanceResponse response = processorChainServiceInstanceService.deleteServiceInstance(request);

        Assertions.assertThat(response).isEqualTo(new DeleteServiceInstanceResponse());
        Mockito.verify(processorChain).delete();
    }


}