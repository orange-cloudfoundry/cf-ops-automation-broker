package com.orange.oss.ondemandbroker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class ProcessorChainServiceInstanceServiceTest {

    @Mock
    private
    ProcessorChain processorChain;

    @InjectMocks
    private
    ProcessorChainServiceInstanceService service;

    @Test
    public void chains_create_processors_on_service_instance_creation() {
        //given
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder().build();

        //when
        CreateServiceInstanceResponse response = service.createServiceInstance(request);

        //then the default response is returned
        Assertions.assertThat(response).isEqualTo(CreateServiceInstanceResponse.builder().build());

        //and call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(CreateServiceInstanceResponse.builder().build());
        Mockito.verify(processorChain).create(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);
    }

   @Test
    public void creates_method_logs_and_rethrows_exceptions() {
       RuntimeException confidentialException = new RuntimeException("unable to push at https://login:pwd@mygit.site.org/secret_path", new IOException());
       CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder().build();
       //given a processor throws an exception
       Mockito.doThrow(confidentialException).when(processorChain).create(any(Context.class));

       //when
       assertThrows(RuntimeException.class, () ->
           service.createServiceInstance(request));
       //then exception is logged and rethrown
    }

    @Test
    public void uses_create_response_from_context_when_set() {
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder().build();

        //given a processor that generates a response into the context
        final CreateServiceInstanceResponse customResponse = CreateServiceInstanceResponse.builder()
                .dashboardUrl("https://a.dashboard.org")
                .build();

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
    public void chains_getLastCreateOperation_processors() {
        //given
        GetLastServiceOperationRequest request = GetLastServiceOperationRequest.builder()
                .serviceInstanceId("instanceId").build();

        //when
        GetLastServiceOperationResponse response = service.getLastOperation(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        GetLastServiceOperationResponse expected = GetLastServiceOperationResponse.builder()
                .operationState(OperationState.SUCCEEDED)
                .build();
        Assertions.assertThat(response).isEqualTo(expected);
        Mockito.verify(processorChain).getLastOperation(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST)).isEqualTo(request);
    }

    @Test
    public void uses_last_create_response_from_context_when_set() {
        //given

        GetLastServiceOperationRequest request = GetLastServiceOperationRequest.builder()
                .serviceInstanceId("instanceId")
                .build();

        //given a processor that generates a response into the context
        final GetLastServiceOperationResponse customResponse = GetLastServiceOperationResponse.builder()
                .description("progress 5%")
                .build();

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
    public void chains_delete_processors_on_service_instance_deletion() {
        //given

        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //when
        DeleteServiceInstanceResponse response = service.deleteServiceInstance(request);
        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(processorChain).delete(argument.capture());
        //and default response is returned
        Assertions.assertThat(response).isEqualTo(DeleteServiceInstanceResponse.builder().build());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);

    }

    @Test
    public void uses_delete_response_from_context_when_set() {
        //given
        DeleteServiceInstanceRequest request = OsbBuilderHelper.aDeleteServiceInstanceRequest();

        //given a processor that generates a response into the context
        final DeleteServiceInstanceResponse customResponse = DeleteServiceInstanceResponse.builder()
                .async(true)
                .operation("state")
                .build();

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
    public void chains_update_processors_on_service_instance_update() {
        //given
        UpdateServiceInstanceRequest request = OsbBuilderHelper.anUpdateServiceInstanceRequest();
        //when
        UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(UpdateServiceInstanceResponse.builder().build());
        Mockito.verify(processorChain).update(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);

    }

    @Test
    public void uses_update_response_from_context_when_set() {
        //given
        UpdateServiceInstanceRequest request = OsbBuilderHelper.anUpdateServiceInstanceRequest();

        //given a processor that generates a response into the context
        final UpdateServiceInstanceResponse customResponse = UpdateServiceInstanceResponse.builder()
                .async(true)
                .operation("state")
                .build();

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