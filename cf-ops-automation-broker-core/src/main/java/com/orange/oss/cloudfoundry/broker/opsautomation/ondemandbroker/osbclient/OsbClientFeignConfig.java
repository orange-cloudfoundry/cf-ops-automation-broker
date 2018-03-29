/*
 * <!--
 *
 *     Copyright (C) 2015 Orange
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 * -->
 */

package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import feign.Logger;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * Overrides  {@link <a href="http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign">Feign</a>} Defaults
 * for filtered broker web client.
 *
 * @author Sebastien Bortolussi
 */
@Configuration
@Import(FeignClientsConfiguration.class)
public class OsbClientFeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    Logger customFeignLogger() {
        return new Slf4jLogger(ServiceInstanceServiceClient.class);
    }

    /**
     * Override default Gson message converter media type support to support
     * legacy servers sometimes retuning text/plain media type.
     * This overrides spring boot default, see https://github.com/spring-projects/spring-boot/blob/3fddfee65c901d2aade6eb8a63781b766657d664/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/http/GsonHttpMessageConvertersConfiguration.java#L50
     */
//    Currently disabled as this has the side effect of changing default spring web Json lib from Jackson to Gson
//    As a side effects, seems the input validation is stronger and this breaks CassandraServiceProvisionningTest
//    rest-assured based client which is not compliant w.r.t. "X-Broker-API-Originating-Identity" mandatory header.
//    @Bean
    public GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(gson);
        List<MediaType> supportedMediaTypes = converter.getSupportedMediaTypes();
        if (! supportedMediaTypes.contains(TEXT_PLAIN)) {
            supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
            supportedMediaTypes.add(TEXT_PLAIN);
            converter.setSupportedMediaTypes(supportedMediaTypes);
        }
        return converter;
    }

    /**
     * Override default Jackson message converter media type support to support
     * legacy servers sometimes retuning text/plain media type.
     * This overrides spring boot default, see https://github.com/spring-projects/spring-boot/blob/3fddfee65c901d2aade6eb8a63781b766657d664/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/http/JacksonHttpMessageConvertersConfiguration.java#L53
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
            ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> supportedMediaTypes = converter.getSupportedMediaTypes();
        if (! supportedMediaTypes.contains(TEXT_PLAIN)) {
            supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
            supportedMediaTypes.add(TEXT_PLAIN);
            converter.setSupportedMediaTypes(supportedMediaTypes);
        }
        return converter;
    }

    /**
     * Turn on reading enums from string to deserialize OSB Pojo enums
     */
    @Autowired
    public void configureJacksonEnumDeserialization(ObjectMapper jackson2ObjectMapper) {
        jackson2ObjectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    @Bean
    OkHttpClient customFeignOkHttpClient(okhttp3.OkHttpClient customOkHttpClient) {
        return new OkHttpClient(customOkHttpClient);
    }

}
