package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;

/**
 * Starts the COAB application, and queries it using the OSB client.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class OsbClientTestApplicationTest {

    @Autowired
    OsbClientFactory clientFactory;

    @LocalServerPort
    int port;


    @Test
    public void constructs_feign_clients() {
        //given
        String url = "http://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";

        //when
        CatalogServiceClient catalogServiceClient = clientFactory.getClient(url, user, password, CatalogServiceClient.class);

        //then
        Catalog catalog = catalogServiceClient.getCatalog();
        assertThat(catalog).isNotNull();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertThat(serviceDefinition).isNotNull();
        Plan defaultPlan = serviceDefinition.getPlans().get(0);
        assertThat(defaultPlan).isNotNull();


        //when
        ServiceInstanceServiceClient serviceInstanceServiceClient= clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

        //then
        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_guid");
        cfContextProps.put("space_guid", "space_guid");
        Map<String, Object> serviceInstanceParams= new HashMap<>();

        Context cfContext = new Context("cloudfoundry", cfContextProps);
        CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
                serviceDefinition.getId(),
                defaultPlan.getId(),
                "org_guid",
                "space_guid",
                cfContext,
                serviceInstanceParams
        );
        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance("service_instance_guid", createServiceInstanceRequest, false);
        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();

        //when
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceBindingServiceClient.class);

        //then
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        BindResource bindResource = new BindResource("app_guid", "aRoute", routeBindingParams);
        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = new CreateServiceInstanceBindingRequest(
                serviceDefinition.getId(),
                defaultPlan.getId(),
                bindResource,
                cfContext,
                serviceBindingParams
        );
        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingServiceClient.createServiceInstanceBinding("service_instance_guid", "service_binding_guid", createServiceInstanceBindingRequest);
        assertThat(bindResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(bindResponse.getBody()).isNotNull();

        Map<String, Object> updateServiceInstanceParams= new HashMap<>();

        UpdateServiceInstanceRequest updateServiceInstanceRequest = new UpdateServiceInstanceRequest(
                serviceDefinition.getId(),
                defaultPlan.getId(),
                updateServiceInstanceParams,
                new UpdateServiceInstanceRequest.PreviousValues(defaultPlan.getId()),
                cfContext
                );
//        ResponseEntity<UpdateServiceInstanceResponse> updateResponse = serviceInstanceServiceClient.updateServiceInstance("service_instance_guid", updateServiceInstanceRequest, false);
//        assertThat(updateResponse.getStatusCode()).isEqualTo(OK);
//        assertThat(updateResponse.getBody()).isNotNull();

        ResponseEntity<String> deleteBindingResponse = serviceInstanceBindingServiceClient.deleteServiceInstanceBinding(
                "service_instance_guid",
                "service_binding_guid",
                serviceDefinition.getId(),
                defaultPlan.getId());
        assertThat(deleteBindingResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteBindingResponse.getBody()).isNotNull();

        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = serviceInstanceServiceClient.deleteServiceInstance(
                "service_instance_guid",
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false);
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();
    }

}
