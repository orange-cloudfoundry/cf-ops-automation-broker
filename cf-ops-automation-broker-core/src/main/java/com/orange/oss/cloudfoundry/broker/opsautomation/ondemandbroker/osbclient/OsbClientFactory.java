package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Dynamically creates FeignClient bound to url/user/password.
 *
 *  Inspired by https://cloud.spring.io/spring-cloud-netflix/single/spring-cloud-netflix.html#_creating_feign_clients_manually
 and https://github.com/spring-cloud/spring-cloud-netflix/blob/64ad2ba20c1fc2b5556cb94386fecd0f4f65a816/spring-cloud-netflix-core/src/main/java/org/springframework/cloud/netflix/feign/FeignClientFactoryBean.java#L93
 */
@Service
public class OsbClientFactory {

    private Feign.Builder builder;
    private Client client;
    private okhttp3.OkHttpClient okHttpClient;
    private Encoder encoder;
    private Decoder decoder;
    private Contract springMvcContract;

    public OsbClientFactory(@Qualifier(value = "customFeignBuilder") Feign.Builder builder,
                            Client client,
                            OkHttpClient okHttpClient,
                            Encoder encoder,
                            Decoder decoder,
                            Contract springMvcContract) {
        this.builder = builder;
        this.client = client;
        this.okHttpClient = okHttpClient;
        this.encoder = encoder;
        this.decoder = decoder;
        this.springMvcContract = springMvcContract;
    }

    public <T> T getClient(String url, String user, String password, Class<T> clientClass) {
        return builder
                .client(new feign.okhttp.OkHttpClient(okHttpClient)) //bypass default client with ribbon that springcloud instanciates by default, failing with "Load balancer does not have available server for client"
                .encoder(encoder)
                .decoder(decoder)
                .contract(springMvcContract) //required to support spring mvc annotations instead of native feign annotations
                .requestInterceptor(new BasicAuthRequestInterceptor(user, password))
                .target(clientClass, url);
    }


}
