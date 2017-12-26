package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.Client;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class OsbClientTestApplicationTest {

    @LocalServerPort
    int port;

    @Autowired
    Feign.Builder builder;
    @Autowired
    Client client;
    @Autowired
    okhttp3.OkHttpClient okHttpClient;
    @Autowired
    Encoder encoder;
    @Autowired
    Decoder decoder;

    CatalogServiceClient catalogServiceClient;
    ServiceInstanceServiceClient serviceInstanceServiceClient;
    ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient;


    @Test
    public void constructs_feign_client() {
        //given
        String url = "http://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";

        //when
        catalogServiceClient = builder
                .client(new feign.okhttp.OkHttpClient(okHttpClient)) //bypass default client with ribbon that springcloud instanciates by default, failing with "Load balancer does not have available server for client"
                .encoder(encoder)
                .decoder(decoder)
                .requestInterceptor(new BasicAuthRequestInterceptor(user, password))
                .target(CatalogServiceClient.class, url);

        //then
        assertThat(catalogServiceClient.getCatalog()).isNotNull();
    }

    @Test
    @Ignore("Need to configure dynamically host in feign client")
    public void invokes_catalog() {
        assertThat(catalogServiceClient.getCatalog()).isNotNull();
    }
}
