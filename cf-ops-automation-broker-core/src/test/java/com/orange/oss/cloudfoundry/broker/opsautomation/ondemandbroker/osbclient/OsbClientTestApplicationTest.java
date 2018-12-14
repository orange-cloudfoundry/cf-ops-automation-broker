package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;
import static org.springframework.http.HttpStatus.*;

/**
 * Verifies our OSB client properly sends queries and parses responses:
 * Starts the COAB application, queries it using the OSB client, and asserts default responses
 * Also works against recorded mocks providing additional coverage of returned responses.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class OsbClientTestApplicationTest {

    @Autowired
    OsbClientFactory clientFactory;

    @LocalServerPort
    int port;


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8088)
            .httpsPort(8089)
            .notifier(new Slf4jNotifier(true))
    );

    @Test
    public void feign_client_is_compatible_with_our_spring_cloud_service_broker_library() throws JsonProcessingException {
        boolean recordLocalServerResponses = false;
        //noinspection ConstantConditions
        if (recordLocalServerResponses) {
            WireMock.startRecording("http://localhost:" + port);
            runAsyncCrudLifeCycle(8088, false, false);
            WireMock.stopRecording();
        } else {
            runAsyncCrudLifeCycle(port, false, false);
        }
    }

    @Test
    public void feign_client_is_compatible_with_recorded_mocks() throws JsonProcessingException {
        runAsyncCrudLifeCycle(8089, true, true //mock have non empty response bodies
        );
    }

    @Test
    public void feign_client_unmarshalls_bind_responses() throws JsonProcessingException {
        //given
        String url = "https://127.0.0.1:" + 8089;
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

        //then
        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_id");
        cfContextProps.put("space_guid", "space_id");

        Context cfContext = CloudFoundryContext.builder().properties(cfContextProps).build();
        String originatingIdentityHeader = buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM);
        String serviceInstanceGuid = "111";



        //when
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceBindingServiceClient.class);

        //then
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = BindResource.builder()
                .appGuid("app_guid")
                .route("aRoute")
                .properties(routeBindingParams)
                .build();

        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = CreateServiceInstanceBindingRequest.builder()
                .serviceInstanceId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .bindResource(bindResource)
                .context(cfContext)
                .parameters(serviceBindingParams)
                .build();

        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingServiceClient.createServiceInstanceBinding(
                serviceInstanceGuid,
                "service_binding_guid",
                false,
                null,
                originatingIdentityHeader,
                createServiceInstanceBindingRequest);
        assertThat(bindResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(bindResponse.getBody()).isNotNull();

        CreateServiceInstanceAppBindingResponse bindingResponse = bindResponse.getBody();
        Map<String, Object> credentials = bindingResponse.getCredentials();
        assertThat(credentials).isNotNull().isNotEmpty();

    }

    @Test
    // Note this is also covered by OsbClientFeignConfigTest
    public void feign_client_unmarshalls_last_operation_responses() throws JsonProcessingException {
        //given
        String url = "https://127.0.0.1:" + 8089;
        String user = "user";
        String password = "secret";

        //and a catalog is fetched (as a prereq)
        CatalogServiceClient catalogServiceClient = clientFactory.getClient(url, user, password, CatalogServiceClient.class);
        Catalog catalog = catalogServiceClient.getCatalog();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        Plan defaultPlan = serviceDefinition.getPlans().get(0);

        //when querying against recorded mock response
        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

        //then

        //Given a parameter request
        CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .serviceInstanceId("service-instance-guid")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .originatingIdentity(OsbBuilderHelper.aCfUserContext())
                .build();

        CloudFoundryContext cfContext = CloudFoundryContext.builder()
                .organizationGuid("org_id")
                .spaceGuid("space_id")
                .build();

        String originatingIdentityHeader = buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM);
        String serviceInstanceGuid = "111";

        //Then response is properly parsed out
        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance(
                serviceInstanceGuid,
                false,
                null,
                originatingIdentityHeader,
                createServiceInstanceRequest
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        CreateServiceInstanceResponse createServiceInstanceResponse = createResponse.getBody();
        assertThat(createServiceInstanceResponse.getOperation()).isEqualTo("a manually crafted opaque string");
    }

    public void runAsyncCrudLifeCycle(int port, boolean useTls, boolean expectNonEmptyResponseBodies) throws JsonProcessingException {
        //given
        String protocol = useTls ? "https" : "http";
        String url = protocol + "://127.0.0.1:" + port;
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
        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

        //then
        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_id");
        cfContextProps.put("space_guid", "space_id");
        Map<String, Object> serviceInstanceParams = new HashMap<>();

        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .serviceInstanceId("service-instance-guid")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .build();
        String originatingIdentityHeader = buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM);
        String serviceInstanceGuid = "111";

        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance(
                serviceInstanceGuid,
                false,
                null,
                originatingIdentityHeader,
                createServiceInstanceRequest
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        if (expectNonEmptyResponseBodies) {
            CreateServiceInstanceResponse createServiceInstanceResponse = createResponse.getBody();
            assertThat(createServiceInstanceResponse.getOperation()).isNotEmpty();
        }

        ResponseEntity<GetLastServiceOperationResponse> lastOperationResponse = serviceInstanceServiceClient.getServiceInstanceLastOperation(
                serviceInstanceGuid,
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
        /////

        Context cfContext = CloudFoundryContext.builder()
            .organizationGuid("org_guid")
            .spaceGuid("space_guid")
            .build();

        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .bindResource(BindResource.builder()
                        .appGuid("app_guid")
                        .route("aRoute")
                        .build())
                .context(cfContext)
                .bindingId("service-instance-binding-id")
                .serviceInstanceId(serviceInstanceGuid)
                .apiInfoLocation("api-info")
                .originatingIdentity(OsbBuilderHelper.aCfUserContext())
                .platformInstanceId("cf-instance-id")
                .build();

        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingServiceClient.createServiceInstanceBinding(
                serviceInstanceGuid,
                "service_binding_guid",
                false,
                null,
                originatingIdentityHeader,
                createServiceInstanceBindingRequest);
        assertThat(bindResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(bindResponse.getBody()).isNotNull();
        if (expectNonEmptyResponseBodies) {
            CreateServiceInstanceAppBindingResponse bindingResponse = bindResponse.getBody();
            Map<String, Object> credentials = bindingResponse.getCredentials();
            assertThat(credentials).isNotNull().isNotEmpty();
        }

        Map<String, Object> updateServiceInstanceParams = new HashMap<>();

        ////
        // Given an incoming delete request
        UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .serviceInstanceId(serviceInstanceGuid)
                .build();


        ResponseEntity<UpdateServiceInstanceResponse> updateResponse = serviceInstanceServiceClient.updateServiceInstance(
                serviceInstanceGuid,
                false,
                null,
                originatingIdentityHeader,
                updateServiceInstanceRequest);
        assertThat(updateResponse.getStatusCode()).isEqualTo(OK);
        assertThat(updateResponse.getBody()).isNotNull();

        ResponseEntity<String> deleteBindingResponse = serviceInstanceBindingServiceClient.deleteServiceInstanceBinding(
                serviceInstanceGuid,
                "service_binding_guid",
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false,
                null,
                originatingIdentityHeader);
        assertThat(deleteBindingResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteBindingResponse.getBody()).isNotNull();

        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = serviceInstanceServiceClient.deleteServiceInstance(
                serviceInstanceGuid,
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false,
                null,
                originatingIdentityHeader);
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();
    }


    @Test
    public void feign_client_is_compatible_with_previous_spring_cloud_service_broker_library() throws JsonProcessingException {
        //given
        String url = "https://127.0.0.1:" + 8089;
        String user = "user";
        String password = "secret";

        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

//        /v2/service_instances/111?service_id=cassandra-service-broker&plan_id=cassandra-plan&accepts_incomplete


        //when
        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = serviceInstanceServiceClient.deleteServiceInstance(
                "111",
                "cassandra-service-broker",
                "cassandra-plan",
                false,
                null,
                buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM));

        //then
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void feign_client_handles_server_500_response() throws JsonProcessingException {
        //given
        String url = "https://127.0.0.1:" + 8089;
        String user = "user";
        String password = "secret";

        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

        //when
        CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("cassandra-service-broker")
                .planId("cassandra-plan")
                .serviceInstanceId("222")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .build();

        //Then expect
        thrown.expect(FeignException.class);
        thrown.expectMessage("status 500 reading ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest); content:\n" +
                "{\"description\":\"Keyspace ks111 already exists\"}");

        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance(
                "222",
                false,
                null,
                buildOriginatingIdentityHeader("a_user_guid", CLOUD_FOUNDRY_PLATFORM),
                createServiceInstanceRequest);

        //then
        assertThat(createResponse.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(createResponse.getBody()).isNotNull();

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
