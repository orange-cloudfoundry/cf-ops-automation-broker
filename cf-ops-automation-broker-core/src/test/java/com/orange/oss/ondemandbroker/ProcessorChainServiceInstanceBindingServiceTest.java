package com.orange.oss.ondemandbroker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ProcessorChainServiceInstanceBindingServiceTest {

    @Mock
    private
    ProcessorChain processorChain;

    @InjectMocks
    private
    ProcessorChainServiceInstanceBindingService service;

    @Test
    public void chains_bind_processors_on_service_instance_binding() {
        //given
        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder().build();

        //when
        CreateServiceInstanceBindingResponse response = service.createServiceInstanceBinding(request);

        //then the default response is returned
        Assertions.assertThat(response).isEqualTo(CreateServiceInstanceAppBindingResponse.builder().build());
        //and call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(CreateServiceInstanceAppBindingResponse.builder().build());
        Mockito.verify(processorChain).bind(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_REQUEST)).isEqualTo(request);
    }

    @Test
    public void creates_method_logs_and_rethrows_exceptions() {
        RuntimeException confidentialException = new RuntimeException("unable to push at https://login:pwd@mygit.site.org/secret_path", new IOException());
        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder().build();
        //given a processor throws an exception
        Mockito.doThrow(confidentialException).when(processorChain).bind(any(Context.class));

        //when
        assertThrows(RuntimeException.class, () ->
            service.createServiceInstanceBinding(request));
        //then exception is logged and rethrown
    }


    @Test
    public void uses_create_response_from_context_when_set() {
        CreateServiceInstanceBindingRequest  request = CreateServiceInstanceBindingRequest.builder().build();

        //given a processor that generates a response into the context
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        final CreateServiceInstanceBindingResponse customResponse =
                CreateServiceInstanceAppBindingResponse.builder().credentials(credentials).build();

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preBind(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        service = new ProcessorChainServiceInstanceBindingService(processorChain);


        //when
        CreateServiceInstanceBindingResponse response = service.createServiceInstanceBinding(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }



    @Test
    public void chains_unbind_processors_on_service_instance_unbinding() {
        //given
        DeleteServiceInstanceBindingRequest request = OsbBuilderHelper.anUnbindRequest("service-instance-id", "service-binding-id");

        //when
        service.deleteServiceInstanceBinding(request);

        //then call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(processorChain).unBind(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceBindingService.DELETE_SERVICE_INSTANCE_BINDING_REQUEST)).isEqualTo(request);
    }


    public ProcessorChain aProcessorChain(BrokerProcessor processor) {
        List<BrokerProcessor> processors= new ArrayList<>();
        processors.add(new DefaultBrokerProcessor());
        processors.add(processor);
        DefaultBrokerSink sink=new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }


}