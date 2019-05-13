package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import java.util.HashMap;
import java.util.Map;


/**
 * When our jitpack forks don't reload properly in IDE, run this helper main from shell, and tweak classpath to diagnose
 * whether issue really comes from IDE or not (point to locally built artifacts)
 */
public class MavenDependencyHellMain {

    public static void main(String[] args) {
        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .parameters(parameters)
                .serviceInstanceId("service-instance-guid")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            System.out.println(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}