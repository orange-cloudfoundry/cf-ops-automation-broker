package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl.*;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

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
        ServiceInstanceBindingServiceClient expectedServiceInstanceBindingServiceClient = mock(ServiceInstanceBindingServiceClient.class);
        when(clientFactory.getClient(anyString(), anyString(), anyString(), eq(ServiceInstanceBindingServiceClient.class))).thenReturn(expectedServiceInstanceBindingServiceClient);

        //when
        CatalogServiceClient catalogServiceClient = osbProxy.constructCatalogClient("https://service-instance-id-cassandra-broker.mydomain/com");
        ServiceInstanceServiceClient serviceInstanceServiceClient = osbProxy.constructServiceInstanceServiceClient("https://service-instance-id-cassandra-broker.mydomain/com");
        ServiceInstanceBindingServiceClient serviceInstanceServiceBindingClient = osbProxy.constructServiceInstanceBindingServiceClient("https://service-instance-id-cassandra-broker.mydomain/com");

        //then
        assertThat(catalogServiceClient).isSameAs(expectedCatalogServiceClient);
        assertThat(serviceInstanceServiceClient).isSameAs(expectedServiceInstanceServiceClient);
        assertThat(serviceInstanceServiceBindingClient).isSameAs(expectedServiceInstanceBindingServiceClient);
    }


    @Test
    public void maps_provision_request_to_1st_service_and_1st_plan() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        Catalog catalog = new Catalog(Collections.singletonList(serviceDefinition));

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("keyspace_name", "foo");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", parameters);
        request.withServiceInstanceId("service-instance-id");
        request.withApiInfoLocation("api-location");
        request.withCfInstanceId("cf-instance-id");
        request.withOriginatingIdentity(aContext());
        CreateServiceInstanceRequest mappedRequest = osbProxy.mapProvisionRequest(request, catalog);

        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("service-instance-id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getCfInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
        assertThat(mappedRequest.getParameters()).isEqualTo(parameters);
    }

    @Test
    public void maps_bind_request_to_1st_service_and_1st_plan() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        Catalog catalog = new Catalog(Collections.singletonList(serviceDefinition));

        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = new BindResource("app_guid", null, routeBindingParams);

        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_guid");
        cfContextProps.put("space_guid", "space_guid");

        Context cfContext = new Context(CLOUD_FOUNDRY_PLATFORM, cfContextProps);

        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(
                "coab-serviceid",
                "coab-planid",
                bindResource,
                cfContext,
                serviceBindingParams
        );
        request.withBindingId("service-instance-binding-id");
        request.withServiceInstanceId("service-instance-id");
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");

        CreateServiceInstanceBindingRequest mappedRequest = osbProxy.mapBindRequest(request, catalog);

        assertThat(mappedRequest.getBindingId()).isEqualTo("service-instance-binding-id");
        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("service-instance-id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-info");
        assertThat(mappedRequest.getCfInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
        assertThat(mappedRequest.getParameters()).isEqualTo(serviceBindingParams);
    }

    @Test
    public void maps_deprovision_request_to_1st_service_and_1st_plan() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        Catalog catalog = new Catalog(Collections.singletonList(serviceDefinition));

        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("service-instance-id", "coab-serviceid", "coab-planid", serviceDefinition, true);
        request.withApiInfoLocation("api-location");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");

        DeleteServiceInstanceRequest mappedRequest = osbProxy.mapDeprovisionRequest(request, catalog);

        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("service-instance-id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getCfInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
    }

    @Test
    public void maps_unbind_request_to_1st_service_and_1st_plan() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        Catalog catalog = new Catalog(Collections.singletonList(serviceDefinition));

        DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest("service-instance-id", "service-binding-id","coab-serviceid", "coab-planid", serviceDefinition);
        request.withApiInfoLocation("api-location");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");

        DeleteServiceInstanceBindingRequest mappedRequest = osbProxy.mapUnbindRequest(request, catalog);

        assertThat(mappedRequest.getBindingId()).isEqualTo("service-binding-id");
        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("service-instance-id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getCfInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
    }

    @Test
    public void serializes_osb_context() {
        Context context = aContext();
        String header = osbProxy.buildOriginatingIdentityHeader(context);

        assertThat(header).isEqualTo(aContextOriginatingHeader());
    }

    @Test
    public void serializes_empty_osb_context() {
        String header = osbProxy.buildOriginatingIdentityHeader(null);

        assertThat(header).isNull();
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
                false, //for now OsbProxyImpl expects sync broker response
                "api-info",
                aContextOriginatingHeader(),
                request);
    }

    @Test
    public void delegates_bind_call() {
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = new BindResource("app_guid", null, routeBindingParams);

        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_guid");
        cfContextProps.put("space_guid", "space_guid");

        Context cfContext = new Context(CLOUD_FOUNDRY_PLATFORM, cfContextProps);

        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(
                "coab-serviceid",
                "coab-planid",
                bindResource,
                cfContext,
                serviceBindingParams
        );
        request.withBindingId("service-instance-binding-id");
        request.withServiceInstanceId("service-instance-id");
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());

        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = mock(ServiceInstanceBindingServiceClient.class);
        //noinspection unchecked
        ResponseEntity<CreateServiceInstanceAppBindingResponse> responseEntity = osbProxy.delegateBind(request, serviceInstanceBindingServiceClient);

        verify(serviceInstanceBindingServiceClient).createServiceInstanceBinding(
                "service-instance-id",
                "service-instance-binding-id",
                "api-info",
                aContextOriginatingHeader(),
                request);
    }


    @Test
    public void delegates_deprovision_call() {
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("service-instance-id", "coab-serviceid", "coab-planid", serviceDefinition, true);
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        ServiceInstanceServiceClient serviceInstanceServiceClient = mock(ServiceInstanceServiceClient.class);
        @SuppressWarnings("unchecked") ResponseEntity<DeleteServiceInstanceResponse> responseEntity = osbProxy.delegateDeprovision(request, serviceInstanceServiceClient);

        verify(serviceInstanceServiceClient).deleteServiceInstance(
                "service-instance-id",
                "coab-serviceid",
                "coab-planid",
                false,
                "api-info",
                aContextOriginatingHeader());
    }

    @Test
    public void delegates_unbind_call() {
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest("service-instance-id", "service-binding-id","coab-serviceid", "coab-planid", serviceDefinition);
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");
        
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = mock(ServiceInstanceBindingServiceClient.class);
        osbProxy.delegateUnbind(request, serviceInstanceBindingServiceClient);

        verify(serviceInstanceBindingServiceClient).deleteServiceInstanceBinding(
                "service-instance-id",
                "service-binding-id",
                "coab-serviceid",
                "coab-planid",
                "api-info",
                aContextOriginatingHeader());
    }



    @Test
    public void maps_successull_deprovision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        DeleteServiceInstanceResponse deleteServiceInstanceResponse = new DeleteServiceInstanceResponse()
                .withAsync(false);
        DeleteServiceInstanceResponse delegatedResponse = new DeleteServiceInstanceResponse()
                .withAsync(false);
        ResponseEntity<DeleteServiceInstanceResponse > delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.OK);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") GetLastServiceOperationResponse mappedResponse = osbProxy.mapDeprovisionResponse(originalResponse, delegatedResponseEnveloppe, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.SUCCEEDED);
        assertThat(mappedResponse.getDescription()).isNull();
        assertThat(mappedResponse.isDeleteOperation()).isTrue();
    }


    @Test
    public void maps_rejected_deprovision_user_facing_responses() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        Response errorResponse = Response.builder()
                .status(HttpStatus.GONE.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"No such service instance 1234\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#deleteServiceInstance(String,String,String,boolean,String,String)", errorResponse);

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapDeprovisionResponse(originalResponse, null, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.FAILED);
        assertThat(mappedResponse.getDescription()).isEqualTo("No such service instance 1234");
        assertThat(mappedResponse.isDeleteOperation()).isTrue();
    }



    @Test
    public void maps_successull_provision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        CreateServiceInstanceResponse delegatedResponse = new CreateServiceInstanceResponse()
                .withAsync(false)
                .withDashboardUrl("https://a-inner-dashboard.com");
        ResponseEntity<CreateServiceInstanceResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.CREATED);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") GetLastServiceOperationResponse mappedResponse = osbProxy.mapProvisionResponse(originalResponse, delegatedResponseEnveloppe, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.SUCCEEDED);
        assertThat(mappedResponse.getDescription()).isNull();
        assertThat(mappedResponse.isDeleteOperation()).isFalse();
    }

    @Test
    public void maps_successull_bind_response() {
        //Given
        CreateServiceInstanceAppBindingResponse delegatedResponse = new CreateServiceInstanceAppBindingResponse();
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        delegatedResponse.withCredentials(credentials);
        delegatedResponse.withBindingExisted(false);

        ResponseEntity<CreateServiceInstanceAppBindingResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.CREATED);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") CreateServiceInstanceAppBindingResponse mappedResponse = osbProxy.mapBindResponse(delegatedResponseEnveloppe, provisionException, aCatalog());


        assertThat(mappedResponse).isEqualTo(delegatedResponse);
    }

    @Test
    public void maps_already_existing_bind_response() {
        //Given
        CreateServiceInstanceAppBindingResponse delegatedResponse = new CreateServiceInstanceAppBindingResponse();
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        delegatedResponse.withCredentials(credentials);
        delegatedResponse.withBindingExisted(true);

        ResponseEntity<CreateServiceInstanceAppBindingResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.OK);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") CreateServiceInstanceAppBindingResponse mappedResponse = osbProxy.mapBindResponse(delegatedResponseEnveloppe, provisionException, aCatalog());


        assertThat(mappedResponse.isBindingExisted()).isTrue();
        assertThat(mappedResponse).isEqualTo(delegatedResponse);
    }

    //

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void maps_rejected_provision_request() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        Response errorReponse = Response.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        thrown.expectMessage(containsString("Missing required fields: keyspace param"));

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapProvisionResponse(originalResponse, null, provisionException, aCatalog());
    }

    @Test
    public void maps_rejected_bind_request() {
        //Given
        Response errorReponse = Response.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceBindingServiceClient#createServiceInstanceBinding(String,String,String,String,CreateServiceInstanceBindingRequest)", errorReponse);

        thrown.expectMessage(containsString("Missing required fields: keyspace param"));

        //when

        osbProxy.mapBindResponse(null, provisionException, aCatalog());
    }

    @Test
    public void maps_feign_client_unknown_errors_to_500_errors() {
        //Given
        Response errorReponse = Response.builder()
                .status(HttpStatus.INSUFFICIENT_STORAGE.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        //when
        RuntimeException mapClientException = osbProxy.mapClientException(provisionException);

        assertThat(mapClientException.getMessage()).isEqualTo("Missing required fields: keyspace param. Original status: 507");
        assertThat(mapClientException.getClass().getAnnotation(ResponseStatus.class).value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void maps_feign_client_known_errors() {
        assertDescriptionAndStatusMapped(HttpStatus.BAD_REQUEST, "Missing required fields: keyspace param");
        assertDescriptionAndStatusMapped(HttpStatus.CONFLICT, "Service instance with id 1234 already exists");
        //should better really map this to error when we get more time.
        assertDescriptionAndStatusMapped(HttpStatus.UNPROCESSABLE_ENTITY, "This Service Plan requires client support for asynchronous service operations.");
    }

    private void assertDescriptionAndStatusMapped(HttpStatus httpStatus, String description) {
        //given
        Response errorReponse = Response.builder()
                .status(httpStatus.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"" + description + "\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        //when
        RuntimeException mapClientException = osbProxy.mapClientException(provisionException);

        //then
        assertThat(mapClientException.getMessage()).isEqualTo(description);
        assertThat(mapClientException.getClass().getAnnotation(ResponseStatus.class).value()).isEqualTo(httpStatus);
    }

    @Test
    public void parses_error_response_description_body() {
        Response errorReponse = Response.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        OsbProxyImpl.ErrorMessage errorMessage = osbProxy.parseReponseBody(provisionException);

        assertThat(errorMessage.getDescription()).isEqualTo("Missing required fields: keyspace param");
    }

    @Test
    public void parses_osb_client_exceptions_into_generic_message() {
        //Given
        DecodeException decodeException = new DecodeException("Could not extract response: no suitable HttpMessageConverter found for response type [?] and content type [text/plain;charset=UTF-8]");

        //when
        String description = osbProxy.parseReponseBody(decodeException).getDescription();

        assertThat(description).isEqualTo("Internal error, please contact administrator");
    }

    @Test
    public void maps_async_creation_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        CreateServiceInstanceResponse delegatedResponse = new CreateServiceInstanceResponse()
                .withAsync(false)
                .withDashboardUrl("https://a-inner-dashboard.com");
        ResponseEntity<CreateServiceInstanceResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.ACCEPTED);

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapProvisionResponse(originalResponse, delegatedResponseEnveloppe,null, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.FAILED);
        assertThat(mappedResponse.getDescription()).isEqualTo("Internal error, please contact administrator");
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

    private GetLastServiceOperationResponse aPreviousOnGoingOperation() {
        return new GetLastServiceOperationResponse()
                    .withOperationState(OperationState.IN_PROGRESS)
                    .withDescription(null);
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        return request;
    }

}