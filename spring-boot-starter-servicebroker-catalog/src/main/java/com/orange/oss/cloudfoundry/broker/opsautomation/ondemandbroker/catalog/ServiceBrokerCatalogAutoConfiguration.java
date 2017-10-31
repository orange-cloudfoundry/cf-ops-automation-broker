package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper.ServiceMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link Catalog}.
 *
 * @author Sebastien Bortolussi
 */
@Configuration
@ConditionalOnMissingBean(Catalog.class)
@EnableConfigurationProperties(CatalogProperties.class)
public class ServiceBrokerCatalogAutoConfiguration {

    @Bean
    public Catalog catalog(CatalogProperties properties) {
        final List<ServiceDefinition> serviceDefinitions = ServiceMapper.toServiceDefinitions(properties.getServices());
        return new Catalog(serviceDefinitions);
    }

}
