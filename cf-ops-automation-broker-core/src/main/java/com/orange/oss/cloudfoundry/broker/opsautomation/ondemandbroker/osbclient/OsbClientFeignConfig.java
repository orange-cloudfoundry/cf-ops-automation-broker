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

import feign.Feign;
import feign.Logger;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
    Logger.Level customFeignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    Logger customFeignLogger() {
        return new Slf4jLogger();
    }

    @Bean
    Feign.Builder customFeignBuilder(okhttp3.OkHttpClient customOkHttpClient) {
        return Feign.builder().client(new OkHttpClient(customOkHttpClient));
    }

}
