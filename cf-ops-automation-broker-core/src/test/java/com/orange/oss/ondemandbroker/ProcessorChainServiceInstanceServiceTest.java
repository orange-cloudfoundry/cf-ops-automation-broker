package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceServiceTest {

    @Mock
    private
    ProcessorChain processorChain;

    @InjectMocks
    private
    ProcessorChainServiceInstanceService service;

    @Test
    public void chains_create_processors_on_service_instance_creation() throws Exception {
        //given
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();

        //when
        CreateServiceInstanceResponse response = service.createServiceInstance(request);

        //then the default response is returned
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceResponse());

        //and call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceResponse());
        Mockito.verify(processorChain).create(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);
    }

   @Test(expected = RuntimeException.class)
    public void creates_method_logs_and_rethrows_exceptions() throws Exception {
       RuntimeException confidentialException = new RuntimeException("unable to push at https://login:pwd@mygit.site.org/secret_path", new IOException());
       CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();
       //given a processor throws an exception
       Mockito.doThrow(confidentialException).when(processorChain).create(any(Context.class));

       //when
        CreateServiceInstanceResponse response = service.createServiceInstance(request);

        //then exception is logged and rethrown
    }

    @Test
    public void uses_create_response_from_context_when_set() {
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
        service = new ProcessorChainServiceInstanceService(processorChain);


        //when
        CreateServiceInstanceResponse response = service.createServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }

    @Test
    public void chains_getLastCreateOperation_processors() throws Exception {
        //given
        GetLastServiceOperationRequest request = new GetLastServiceOperationRequest("instanceId");

        //when
        GetLastServiceOperationResponse response = service.getLastOperation(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        GetLastServiceOperationResponse expected = new GetLastServiceOperationResponse();
        expected.withOperationState(OperationState.SUCCEEDED);
        Assertions.assertThat(response).isEqualTo(expected);
        Mockito.verify(processorChain).getLastOperation(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST)).isEqualTo(request);
    }

    @Test
    public void uses_last_create_response_from_context_when_set() {
        //given
        GetLastServiceOperationRequest request = new GetLastServiceOperationRequest("instanceId");

        //given a processor that generates a response into the context
        final GetLastServiceOperationResponse customResponse = new GetLastServiceOperationResponse()
                .withDescription("progress 5%");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preGetLastOperation(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        service = new ProcessorChainServiceInstanceService(processorChain);


        //when
        GetLastServiceOperationResponse response = service.getLastOperation(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }


    @Test
    public void chains_delete_processors_on_service_instance_deletion() throws Exception {
        //given
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);

        //when
        DeleteServiceInstanceResponse response = service.deleteServiceInstance(request);
        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(processorChain).delete(argument.capture());
        //and default response is returned
        Assertions.assertThat(response).isEqualTo(new DeleteServiceInstanceResponse());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);

    }

    @Test
    public void uses_delete_response_from_context_when_set() {
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
        service = new ProcessorChainServiceInstanceService(processorChain);


        //when
        DeleteServiceInstanceResponse response = service.deleteServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }

    @Test
    public void chains_update_processors_on_service_instance_update() throws Exception {
        //given
        UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(
                "service_id",
                "plan_id",
                new HashMap<>());

        //when
        UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new UpdateServiceInstanceResponse());
        Mockito.verify(processorChain).update(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);

    }

    @Test
    public void uses_update_response_from_context_when_set() {
        //given
        UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(
                "service_id",
                "plan_id",
                new HashMap<>());

        //given a processor that generates a response into the context
        final UpdateServiceInstanceResponse customResponse = new UpdateServiceInstanceResponse()
                .withAsync(true)
                .withOperation("state");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preUpdate(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        service = new ProcessorChainServiceInstanceService(processorChain);


        //when
        UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

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