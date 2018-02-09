package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.FeignException;
import feign.Response;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl.*;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * - construct OSB client: construct url from serviceInstanceId, and configured static pwd
 * - fetch catalog
 * - map req
 * - provisionning instance
 * - map resp
 */
public class OsbProxyImplTest {

    private OsbClientFactory clientFactory = mock(OsbClientFactory.class);
    private OsbProxyImpl osbProxy = new OsbProxyImpl<>("user", "password", "https://{0}-cassandra-broker.mydomain/com", clientFactory);
    GetLastServiceOperationRequest pollingRequest;
    private CreateServiceInstanceRequest request = aCreateServiceInstanceRequest();
    GetLastServiceOperationResponse response;


    @Test
    public void constructs_broker_url_osb_client() {
        String url = osbProxy.getBrokerUrl(request.getServiceInstanceId());
        assertThat(url).isEqualTo("https://service-instance-id-cassandra-broker.mydomain/com");
    }


    /**
     * Feel free to simplify this test if this
     */
    @Test
    public void constructs_osb_clients() {
        //given
        ServiceInstanceServiceClient expectedServiceInstanceServiceClient = mock(ServiceInstanceServiceClient.class);
        when(clientFactory.getClient(anyString(), anyString(), anyString(), eq(ServiceInstanceServiceClient.class))).thenReturn(expectedServiceInstanceServiceClient);
        CatalogServiceClient expectedCatalogServiceClient = mock(CatalogServiceClient.class);
        when(clientFactory.getClient(anyString(), anyString(), anyString(), eq(CatalogServiceClient.class))).thenReturn(expectedCatalogServiceClient);

        //when
        CatalogServiceClient catalogServiceClient = osbProxy.constructCatalogClient("https://service-instance-id-cassandra-broker.mydomain/com");
        ServiceInstanceServiceClient serviceInstanceServiceClient = osbProxy.constructServiceInstanceServiceClient("https://service-instance-id-cassandra-broker.mydomain/com");

        //then
        assertThat(catalogServiceClient).isSameAs(expectedCatalogServiceClient);
        assertThat(serviceInstanceServiceClient).isSameAs(expectedServiceInstanceServiceClient);
    }


    @Test
    public void maps_request_to_1st_service_and_1st_plan() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        Catalog catalog = new Catalog(Collections.singletonList(serviceDefinition));

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        CreateServiceInstanceRequest mappedRequest = osbProxy.mapRequest(request, catalog);

        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
    }

    @Test
    public void serializes_osb_context() {
        Context context = aContext();
        String header = osbProxy.buildOriginatingIdentityHeader(context);

        assertThat(header).isEqualTo(aContextOriginatingHeader());
    }

    private String aContextOriginatingHeader() {
        return "cloudfoundry eyJ1c2VyX2lkIjoidXNlcl9ndWlkIiwiZW1haWwiOiJ1c2VyX2VtYWlsIn0=";
    }

    private Catalog aCatalog() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        return new Catalog(Collections.singletonList(serviceDefinition));
    }

    private Context aContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid");
        properties.put(ORIGINATING_EMAIL_KEY, "user_email");
        return new Context(ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties);
    }

    @Test
    public void delegates_provision_call() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        request.withAsyncAccepted(true);
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        ServiceInstanceServiceClient serviceInstanceServiceClient = mock(ServiceInstanceServiceClient.class);
        osbProxy.delegateProvision(request, serviceInstanceServiceClient);

        verify(serviceInstanceServiceClient).createServiceInstance(
                "service-instance-id",
                true,
                "api-info",
                aContextOriginatingHeader(),
                request);
    }

    @Test
    public void maps_successull_provision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.IN_PROGRESS)
                .withDescription(null);
        CreateServiceInstanceResponse delegatedResponse = new CreateServiceInstanceResponse()
                .withAsync(false)
                .withDashboardUrl("https://a-inner-dashboard.com");
        ResponseEntity<CreateServiceInstanceResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.CREATED);
        FeignException provisionException = null;
        Catalog catalog = aCatalog();

        //when
        @SuppressWarnings("ConstantConditions") GetLastServiceOperationResponse mappedResponse = osbProxy.mapResponse(originalResponse, delegatedResponseEnveloppe, provisionException, catalog);

        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.SUCCEEDED)
                .withDescription(null);
        assertThat(mappedResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void maps_rejected_provision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.IN_PROGRESS)
                .withDescription(null);
        Response errorReponse = Response.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);
        Catalog catalog = aCatalog();

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapResponse(originalResponse, null, provisionException, catalog);

        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.FAILED)
                .withDescription("Missing required fields: keyspace param");
        assertThat(mappedResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void parses_error_response_body() {
        OsbProxyImpl.ErrorMessage errorMessage = osbProxy.parseReponseBody("status 422 reading ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest); content:\n" +
                "{\"description\":\"Missing required fields: keyspace param\"}");

        assertThat(errorMessage.getDescription()).isEqualTo("Missing required fields: keyspace param");
    }

    @Test
    @Ignore
    public void maps_async_creation_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.IN_PROGRESS)
                .withDescription(null);
        Response createdReponse = Response.builder()
                .status(HttpStatus.CREATED.value())
                .headers(new HashMap<>())
                .body("", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", createdReponse);
        Catalog catalog = aCatalog();

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapResponse(originalResponse, null, provisionException, catalog);

        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse()
                .withOperationState(OperationState.FAILED)
                .withDescription("Internal error, please contact administrator");
        assertThat(mappedResponse).isEqualTo(expectedResponse);
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        return request;
    }

}