package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        OsbClientFeignConfig.class,
        JacksonAutoConfiguration.class, OkHttpClientConfig.class // explicitly import those as this test does not pull in springboot mechanics in order to execute faster
})
public class OsbClientFeignConfigTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void deserializes_parent_fields_from_CreateServiceInstanceResponse() throws IOException {
        String json = "{\"operation\":\"{\\\"org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest\\\":{\\\"serviceDefinitionId\\\":\\\"cassandra-ondemand-service\\\",\\\"planId\\\":\\\"cassandra-ondemand-plan\\\",\\\"organizationGuid\\\":\\\"org_id\\\",\\\"spaceGuid\\\":\\\"space_id\\\",\\\"serviceInstanceId\\\":\\\"111\\\",\\\"serviceDefinition\\\":{\\\"id\\\":\\\"cassandra-ondemand-service\\\",\\\"name\\\":\\\"cassandra-ondemand\\\",\\\"description\\\":\\\"On demand cassandra dedicated clusters\\\",\\\"bindable\\\":true,\\\"planUpdateable\\\":true,\\\"plans\\\":[{\\\"id\\\":\\\"cassandra-ondemand-plan\\\",\\\"name\\\":\\\"default\\\",\\\"description\\\":\\\"This is a default ondemand plan.  All services are created equally.\\\",\\\"metadata\\\":{},\\\"free\\\":true}],\\\"tags\\\":[\\\"ondemand\\\",\\\"document\\\"],\\\"metadata\\\":{\\\"displayName\\\":\\\"ondemand\\\",\\\"imageUrl\\\":\\\"https://orange.com/image.png\\\",\\\"longDescription\\\":\\\"A dedicated on-demand cassandra cluster\\\",\\\"providerDisplayName\\\":\\\"Orange\\\",\\\"documentationUrl\\\":\\\"https://orange.com/doc\\\",\\\"supportUrl\\\":\\\"https://orange.com/support\\\"}},\\\"parameters\\\":{},\\\"context\\\":{\\\"platform\\\":\\\"cloudfoundry\\\",\\\"properties\\\":{}},\\\"asyncAccepted\\\":true,\\\"apiInfoLocation\\\":\\\"api-info\\\",\\\"originatingIdentity\\\":{\\\"platform\\\":\\\"cloudfoundry\\\",\\\"properties\\\":{\\\"user_id\\\":\\\"user_guid\\\",\\\"email\\\":\\\"user_email\\\"}}},\\\"startRequestDate\\\":\\\"2018-06-07T16:55:48.121Z\\\"}\"}";
        CreateServiceInstanceResponse createServiceInstanceResponse = objectMapper.readValue(json, CreateServiceInstanceResponse.class);
        assertThat(createServiceInstanceResponse.getOperation()).contains("CreateServiceInstanceRequest");
    }

}