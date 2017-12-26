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
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
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
    @Autowired
    SpringMvcContract springMvcContract;

    CatalogServiceClient catalogServiceClient;
    ServiceInstanceServiceClient serviceInstanceServiceClient;
    ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient;


    @Test
    public void constructs_feign_clients() {
        //given
        String url = "http://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";

        //Inspired by https://cloud.spring.io/spring-cloud-netflix/single/spring-cloud-netflix.html#_creating_feign_clients_manually
        //and https://github.com/spring-cloud/spring-cloud-netflix/blob/64ad2ba20c1fc2b5556cb94386fecd0f4f65a816/spring-cloud-netflix-core/src/main/java/org/springframework/cloud/netflix/feign/FeignClientFactoryBean.java#L93
        //when
        catalogServiceClient = builder
                .client(new feign.okhttp.OkHttpClient(okHttpClient)) //bypass default client with ribbon that springcloud instanciates by default, failing with "Load balancer does not have available server for client"
                .encoder(encoder)
                .decoder(decoder)
                .contract(springMvcContract) //required to support spring mvc annotations instead of native feign annotations
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
