package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants;
import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
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
 *
 * Live mode:
 * - Starts the OsbClientTestApplication application (which returns default OSB reponses),
 *    queries it using the OSB client, and asserts default (mostly empty) responses
 *
 * Mocked response mode:
 * - queries are asserted using wiremock expectation in resources/mappings files
 * - responses are returned by wiremock, the parsed response asserts are done in each tests
 * - mocked responses and be updated by turning on the recording. Manual diff/edition is then
 * required to preserve non default responses.
 *
 * Troubleshooting steps: <br>
 * - run the OsbClientTestApplication spring boot app using ide <br>
 * - invoke some manual-OsbClientTestApplication-curls.bash commands to diagnose
 * - turn on recording to update recorded mocks in resources/mappings + check diff
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {OsbClientTestApplication.class})
public class OsbClientTest {

    public static final String SERVICE_INSTANCE_GUID = "111";
    public static final String SERVICE_BINDING_GUID = "service_binding_guid";
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
    public void feign_client_is_compatible_with_our_current_osb_library() throws JsonProcessingException {
        boolean recordLocalServerResponses = false;
        //noinspection ConstantConditions
        if (recordLocalServerResponses) {
            WireMock.startRecording("http://localhost:" + port);
            try {
                runAsyncCrudLifeCycle(8088, false, false);
            } finally {
                WireMock.stopRecording();
            }
        } else {
            runAsyncCrudLifeCycle(port, false, false);
        }
    }

    @Test
    public void feign_client_is_compatible_with_recorded_mocks() throws JsonProcessingException {
        runAsyncCrudLifeCycle(8089, true, true //mock have non empty response bodies
        );
    }



    public void runAsyncCrudLifeCycle(int port, boolean useTls, boolean expectNonEmptyResponseBodies) throws JsonProcessingException {
        //given
        String protocol = useTls ? "https" : "http";
        String url = protocol + "://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";
        ServiceDefinition serviceDefinition = assertCatalogIsReturned(url, user, password);

        Plan defaultPlan = serviceDefinition.getPlans().get(0);

        assertServiceInstanceRequests(expectNonEmptyResponseBodies, url, user, password, serviceDefinition, defaultPlan);

        assertServiceBindingRequests(expectNonEmptyResponseBodies, url, user, password, serviceDefinition, defaultPlan);
    }

    private void assertServiceBindingRequests(boolean expectNonEmptyResponseBodies, String url, String user, String password, ServiceDefinition serviceDefinition, Plan defaultPlan) throws JsonProcessingException {
        String originatingIdentityHeader = buildOriginatingIdentityHeader();
        //when
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceBindingServiceClient.class);

        //then
        /////

        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .bindResource(BindResource.builder()
                        .appGuid("app_guid")
                        .route("aRoute")
                        .build())
                .context(OsbBuilderHelper.aCfContext())
                .bindingId(SERVICE_BINDING_GUID)
                .serviceInstanceId(SERVICE_INSTANCE_GUID)
                .apiInfoLocation("api-info")
                .originatingIdentity(OsbBuilderHelper.aCfUserContext())
                .platformInstanceId("cf-instance-id")
                .build();

        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingServiceClient.createServiceInstanceBinding(
                SERVICE_INSTANCE_GUID,
                SERVICE_BINDING_GUID,
                false,
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value,
                createServiceInstanceBindingRequest);
        assertThat(bindResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(bindResponse.getBody()).isNotNull();
        if (expectNonEmptyResponseBodies) {
            CreateServiceInstanceAppBindingResponse bindingResponse = bindResponse.getBody();
            Map<String, Object> credentials = bindingResponse.getCredentials();
            assertThat(credentials).isNotNull().isNotEmpty();
        }


        ResponseEntity<String> deleteBindingResponse = serviceInstanceBindingServiceClient.deleteServiceInstanceBinding(
                SERVICE_INSTANCE_GUID,
                SERVICE_BINDING_GUID,
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false,
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value);
        assertThat(deleteBindingResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteBindingResponse.getBody()).isNotNull();
    }

    private String buildOriginatingIdentityHeader() throws JsonProcessingException {
        Map<String, Object> propMap = new HashMap<>();
        propMap.put(ORIGINATING_USER_KEY, "a_user_guid");
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        String properties = mapper.writeValueAsString(propMap);
        String encodedProperties = new String(Base64Utils.encode(properties.getBytes()));
        return CLOUD_FOUNDRY_PLATFORM + " " + encodedProperties;
    }

    private void assertServiceInstanceRequests(boolean expectNonEmptyResponseBodies, String url, String user, String password, ServiceDefinition serviceDefinition, Plan defaultPlan) throws JsonProcessingException {
        //when
        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

        //then

        //Given a parameter request

        CreateServiceInstanceRequest createServiceInstanceRequest = CreateServiceInstanceRequest.builder()
                .serviceInstanceId(SERVICE_INSTANCE_GUID)
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build())
                .originatingIdentity(OsbBuilderHelper.aCfUserContext())
                .build();
        String originatingIdentityHeader = buildOriginatingIdentityHeader();

        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance(
                SERVICE_INSTANCE_GUID,
                false,
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value,
                createServiceInstanceRequest
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        if (expectNonEmptyResponseBodies) {
            CreateServiceInstanceResponse createServiceInstanceResponse = createResponse.getBody();
            assertThat(createServiceInstanceResponse.getOperation()).isNotEmpty();
            assertThat(createServiceInstanceResponse.getOperation()).isEqualTo("a manually crafted opaque string");
        }

        ResponseEntity<GetLastServiceOperationResponse> lastOperationResponse = serviceInstanceServiceClient.getServiceInstanceLastOperation(
                SERVICE_INSTANCE_GUID,
                serviceDefinition.getId(),
                defaultPlan.getId(),
                "an opaque operation string",
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value
        );
        assertThat(lastOperationResponse.getStatusCode()).isEqualTo(OK);
        assertThat(lastOperationResponse.getBody()).isNotNull();



        ////
        // Given an incoming update request
        UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId(serviceDefinition.getId())
                .planId(defaultPlan.getId())
                .serviceInstanceId(SERVICE_INSTANCE_GUID)
                .context(OsbBuilderHelper.aCfContext())
                .build();

        ResponseEntity<UpdateServiceInstanceResponse> updateResponse = serviceInstanceServiceClient.updateServiceInstance(
                SERVICE_INSTANCE_GUID,
                false,
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value,
                updateServiceInstanceRequest);
        assertThat(updateResponse.getStatusCode()).isEqualTo(OK);
        assertThat(updateResponse.getBody()).isNotNull();


        ////
        // Given an incoming delete request
        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = serviceInstanceServiceClient.deleteServiceInstance(
                SERVICE_INSTANCE_GUID,
                serviceDefinition.getId(),
                defaultPlan.getId(),
                false,
                null,
                originatingIdentityHeader,
                OsbConstants.X_Broker_API_Version_Value);
        assertThat(deleteInstanceResponse.getStatusCode()).isEqualTo(OK);
        assertThat(deleteInstanceResponse.getBody()).isNotNull();
    }

    private ServiceDefinition assertCatalogIsReturned(String url, String user, String password) {
        //when
        CatalogServiceClient catalogServiceClient = clientFactory.getClient(url, user, password, CatalogServiceClient.class);

        //then
        Catalog catalog = catalogServiceClient.getCatalog("2.14");
        assertThat(catalog).isNotNull();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertThat(serviceDefinition).isNotNull();
        assertThat(serviceDefinition.getPlans().get(0)).isNotNull();
        return serviceDefinition;
    }


    @Test
    public void feign_client_supports_text_plain_responses() throws JsonProcessingException {

        //given
        String url = "https://127.0.0.1:" + 8089;
        String user = "user";
        String password = "secret";

        ServiceInstanceServiceClient serviceInstanceServiceClient = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);

//        /v2/service_instances/111?service_id=cassandra-service-broker&plan_id=cassandra-plan&accepts_incomplete


        //when
        ResponseEntity<DeleteServiceInstanceResponse> deleteInstanceResponse = serviceInstanceServiceClient.deleteServiceInstance(
                "text-plain-testcase-service-id",
                "cassandra-service-broker",
                "cassandra-plan",
                false,
                null,
                buildOriginatingIdentityHeader(),
                OsbConstants.X_Broker_API_Version_Value);

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
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceInstanceId("222")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .build();

        //Then expect
        thrown.expect(FeignException.class);
        thrown.expectMessage("status 500 reading ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,String,CreateServiceInstanceRequest); content:\n" +
                "{\"description\":\"Keyspace ks111 already exists\"}");

        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceServiceClient.createServiceInstance(
                "222",
                false,
                null,
                buildOriginatingIdentityHeader(),
                OsbConstants.X_Broker_API_Version_Value,
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
