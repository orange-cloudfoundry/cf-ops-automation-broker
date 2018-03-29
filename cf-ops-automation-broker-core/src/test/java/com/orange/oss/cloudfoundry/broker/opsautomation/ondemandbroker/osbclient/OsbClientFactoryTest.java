package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fest.assertions.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest // needed to have springboot starters/onConditionals trigger
public class OsbClientFactoryTest {

    @Autowired
    OsbClientFactory osbClientFactory;

    @Test
    public void creates_client() {
        ServiceInstanceBindingServiceClient client = osbClientFactory.getClient("https://localhost:8080", "dummyUser", "dummyPassword", ServiceInstanceBindingServiceClient.class);
        assertThat(client).isNotNull();
    }

    @Test
    public void feign_clients_are_independent() {
        ServiceInstanceBindingServiceClient client1 = osbClientFactory.getClient("https://localhost:8080", "dummyUser1", "dummyPassword1", ServiceInstanceBindingServiceClient.class);
        ServiceInstanceBindingServiceClient client2 = osbClientFactory.getClient("https://localhost:8080", "dummyUser2", "dummyPassword2", ServiceInstanceBindingServiceClient.class);
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
        //Each clients should have their own authenticator using distinct passwords.
        //Hard to test since the client does not provide accessors.
        //Can be done manually using the debugger. Here is the corresponding watch expression
        //((feign.SynchronousMethodHandler)((LinkedHashMap.Entry)((LinkedHashMap)((FeignInvocationHandler)client1.h).dispatch).entrySet().toArray()[0]).getValue()).requestInterceptors;
    }

}