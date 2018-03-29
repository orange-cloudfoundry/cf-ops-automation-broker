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
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceBindingServiceTest {

    @Mock
    private
    ProcessorChain processorChain;

    @InjectMocks
    private
    ProcessorChainServiceInstanceBindingService service;

    @Test
    public void chains_bind_processors_on_service_instance_binding() throws Exception {
        //given
        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest();

        //when
        CreateServiceInstanceBindingResponse response = service.createServiceInstanceBinding(request);

        //then the default response is returned
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceBindingResponse());
        //and call is properly chained
        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceBindingResponse());
        Mockito.verify(processorChain).bind(argument.capture());

        //and context is populated with the request
        Context ctx=argument.getValue();
        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_REQUEST)).isEqualTo(request);
    }

    @Test(expected = RuntimeException.class)
    public void creates_method_logs_and_rethrows_exceptions() throws Exception {
        RuntimeException confidentialException = new RuntimeException("unable to push at https://login:pwd@mygit.site.org/secret_path", new IOException());
        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest();
        //given a processor throws an exception
        Mockito.doThrow(confidentialException).when(processorChain).bind(any(Context.class));

        //when
        service.createServiceInstanceBinding(request);

        //then exception is logged and rethrown
    }


    @Test
    public void uses_create_response_from_context_when_set() {
        CreateServiceInstanceBindingRequest  request = new CreateServiceInstanceBindingRequest ();

        //given a processor that generates a response into the context
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        final CreateServiceInstanceBindingResponse customResponse = new CreateServiceInstanceAppBindingResponse().withCredentials(credentials);

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
    public void chains_unbind_processors_on_service_instance_unbinding() throws Exception {
        //given
        DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest("instance_id",
                "binding_id",
                "service_definition_id",
                "plan_id",
                new ServiceDefinition());

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