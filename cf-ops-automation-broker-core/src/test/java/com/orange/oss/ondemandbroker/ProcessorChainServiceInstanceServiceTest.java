package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.*;
import org.assertj.core.api.Assertions;
import org.fest.assertions.MapAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceServiceTest {

    @Mock
    ProcessorChain processorChain;

    @InjectMocks
    ProcessorChainServiceInstanceService processorChainServiceInstanceService;

    @Test
    public void should_chain_create_processors_on_service_instance_creation() throws Exception {
        //given
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();

        //when
        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceResponse());
        Mockito.verify(processorChain).create(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);
    }

    @Test
    public void should_use_create_response_from_context_when_set() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();

        //given a processor that generates a response into the context
        final CreateServiceInstanceResponse customResponse = new CreateServiceInstanceResponse()
                .withDashboardUrl("https://a.dashboard.org");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preCreate(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        processorChainServiceInstanceService = new ProcessorChainServiceInstanceService(processorChain);


        //when
        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }

    @Test
    public void should_chain_getLastCreateOperation_processors() throws Exception {
        //given
        GetLastServiceOperationRequest request = new GetLastServiceOperationRequest("instanceId");

        //when
        GetLastServiceOperationResponse response = processorChainServiceInstanceService.getLastOperation(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new GetLastServiceOperationResponse());
        Mockito.verify(processorChain).getLastCreateOperation(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST)).isEqualTo(request);
    }

    @Test
    public void should_use_last_create_response_from_context_when_set() {
        //given
        GetLastServiceOperationRequest request = new GetLastServiceOperationRequest("instanceId");

        //given a processor that generates a response into the context
        final GetLastServiceOperationResponse customResponse = new GetLastServiceOperationResponse()
                .withDescription("progress 5%");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preGetLastCreateOperation(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        processorChainServiceInstanceService = new ProcessorChainServiceInstanceService(processorChain);


        //when
        GetLastServiceOperationResponse response = processorChainServiceInstanceService.getLastOperation(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }


    @Test
    public void should_chain_delete_processors_on_service_instance_deletion() throws Exception {
        //given
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);

        //when
        DeleteServiceInstanceResponse response = processorChainServiceInstanceService.deleteServiceInstance(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new DeleteServiceInstanceResponse());
        Mockito.verify(processorChain).delete(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);

    }

    @Test
    public void should_use_delete_response_from_context_when_set() {
        //given
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);

        //given a processor that generates a response into the context
        final DeleteServiceInstanceResponse customResponse = new DeleteServiceInstanceResponse()
                .withAsync(true)
                .withOperation("state");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preDelete(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        processorChainServiceInstanceService = new ProcessorChainServiceInstanceService(processorChain);


        //when
        DeleteServiceInstanceResponse response = processorChainServiceInstanceService.deleteServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }



    public ProcessorChain aProcessorChain(BrokerProcessor processor) {
        List<BrokerProcessor> processors= new ArrayList<>();
        processors.add(new DefaultBrokerProcessor());
        processors.add(processor);
        DefaultBrokerSink sink=new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }


}