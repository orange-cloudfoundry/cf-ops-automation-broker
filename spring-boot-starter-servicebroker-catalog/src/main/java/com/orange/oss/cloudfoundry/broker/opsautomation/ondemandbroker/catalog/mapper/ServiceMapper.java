package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.ServiceProperties;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service internal mapper
 *
 * @author Sebastien Bortolussi
 */
public class ServiceMapper {

    public static ServiceDefinition toServiceDefinition(ServiceProperties serviceProperties) {
        return new ServiceDefinition(serviceProperties.getId().toString(),
                serviceProperties.getName(),
                serviceProperties.getDescription(),
                serviceProperties.getBindable(),
                serviceProperties.getPlanUpdateable(),
                PlanMapper.toServiceBrokerPlans(serviceProperties.getPlans()),
                serviceProperties.getTags(),
                serviceProperties.getMetadata(),
                serviceProperties.getRequires(),
                null);
    }

    public static List<ServiceDefinition> toServiceDefinitions(List<ServiceProperties> serviceProperties) {
        return serviceProperties.stream()
                .map(ServiceMapper::toServiceDefinition)
                .collect(Collectors.toList());
    }
}
