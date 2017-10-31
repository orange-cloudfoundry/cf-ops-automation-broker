package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.PlanProperties;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service plan internal mapper
 *
 * @author Sebastien Bortolussi
 */
public class PlanMapper {


    public static org.springframework.cloud.servicebroker.model.Plan toServiceBrokerPlan(PlanProperties planProperties) {

        return new org.springframework.cloud.servicebroker.model.Plan(planProperties.getId(),
                planProperties.getName(),
                planProperties.getDescription(),
                planProperties.getMetadata() ,
                planProperties.getFree());
    }

    public static List<org.springframework.cloud.servicebroker.model.Plan> toServiceBrokerPlans(List<PlanProperties> planProperties) {
        return planProperties.stream()
                .map(PlanMapper::toServiceBrokerPlan)
                .collect(Collectors.toList());
    }
}
