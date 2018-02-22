package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

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
    public void feign_client_is_compatible_with_current_spring_cloud_service_broker_library() throws JsonProcessingException {
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

        Context cfContext = new Context(CLOUD_FOUNDRY_PLATFORM, cfContextProps);
        CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
                serviceDefinition.getId(),
                defaultPlan.getId(),
                "org_guid",
                "space_guid",
                cfContext,
                serviceInstanceParams
        );
        String originatingIdentityHeader = buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM);

        @SuppressWarnings("unchecked")
        ResponseEntity<CreateServiceInstanceResponse> createResponse = (ResponseEntity<CreateServiceInstanceResponse>) serviceInstanceServiceClient.createServiceInstance(
                "service_instance_guid",
                false,
                null,
                originatingIdentityHeader,
                createServiceInstanceRequest
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        ResponseEntity<GetLastServiceOperationResponse> lastOperationResponse = (ResponseEntity<GetLastServiceOperationResponse>) serviceInstanceServiceClient.getServiceInstanceLastOperation(
                "service_instance_guid",
                serviceDefinition.getId(),
                defaultPlan.getId(),
                "an opaque operation string",
                null,
                originatingIdentityHeader
        );
        assertThat(lastOperationResponse.getStatusCode()).isEqualTo(OK);
        assertThat(lastOperationResponse.getBody()).isNotNull();

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
        @SuppressWarnings("unchecked")
        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = (ResponseEntity<CreateServiceInstanceAppBindingResponse>) serviceInstanceBindingServiceClient.createServiceInstanceBinding(
                "service_instance_guid",
                "service_binding_guid",
                null,
                originatingIdentityHeader,
                createServiceInstanceBindingRequest);
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
        @SuppressWarnings("unchecked")
        ResponseEntity<UpdateServiceInstanceResponse> updateResponse = (ResponseEntity<UpdateServiceInstanceResponse>) serviceInstanceServiceClient.updateServiceInstance(
                "service_instance_guid",
                false,
                null,
                originatingIdentityHeader,
                updateServiceInstanceRequest);
        assertThat(updateResponse.getStatusCode()).isEqualTo(OK);
        assertThat(updateResponse.getBody()).isNotNull();

        ResponseEntity<String> deleteBindingResponse = serviceInstanceBindingServiceClient.deleteServiceInstanceBinding(
                "service_instance_guid",
                "service_binding_guid",
                serviceDefinition.getId(),
                defaultPlan.getId(),
                null,
                originatingIdentityHeader);
        assertThat(deleteBindingResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteBindingResponse.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = (ResponseEntity<DeleteServiceInstanceResponse>) serviceInstanceServiceClient.deleteServiceInstance(
                "service_instance_guid",
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false,
                null,
                originatingIdentityHeader);
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();
    }


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8088)
            .httpsPort(8089)
            .notifier(new Slf4jNotifier(true))
    );

    @Test
    public void feign_client_is_compatible_with_previous_spring_cloud_service_broker_library() throws JsonProcessingException {
        //given
        String url = "https://127.0.0.1:" + 8089;
        String user = "user";
        String password = "secret";

        ServiceInstanceServiceClient serviceInstanceServiceClient= clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

//        /v2/service_instances/111?service_id=cassandra-service-broker&plan_id=cassandra-plan&accepts_incomplete


        //when
        @SuppressWarnings("unchecked")
        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = (ResponseEntity<DeleteServiceInstanceResponse>) serviceInstanceServiceClient.deleteServiceInstance(
                "111",
                "cassandra-service-broker",
                "cassandra-plan",
                false,
                null,
                buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM));
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();

    }

    private static final String ORIGINATING_USER_KEY = "user_id";


    /**
     * Inspired from org.springframework.cloud.servicebroker.autoconfigure.web.servlet.ControllerIntegrationTest
     * See https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/c56080e5ec8ed97ba8fe4e15ac2031073fbc45ae/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/servlet/ControllerIntegrationTest.java#L36-L43
     */
    private String buildOriginatingIdentityHeader(@SuppressWarnings("SameParameterValue") String originatingUserGuid, String originatingIdentityPlatform) throws JsonProcessingException {
        Map<String, Object> propMap = new HashMap<>();
        propMap.put(ORIGINATING_USER_KEY, originatingUserGuid);
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        String properties = mapper.writeValueAsString(propMap);
        String encodedProperties = new String(Base64Utils.encode(properties.getBytes()));
        return originatingIdentityPlatform + " " + encodedProperties;
    }


}
