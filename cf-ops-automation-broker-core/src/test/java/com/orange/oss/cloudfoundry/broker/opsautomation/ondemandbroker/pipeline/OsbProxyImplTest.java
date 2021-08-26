package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.DecodeException;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * - construct OSB client: construct url from serviceInstanceId, and configured static pwd
 * - fetch catalog
 * - map req
 * - provisionning instance
 * - map resp
 */
public class OsbProxyImplTest {

    private final OsbClientFactory clientFactory = mock(OsbClientFactory.class);
    private final OsbProxyImpl osbProxy = new OsbProxyImpl("user", "password", "https://{0}-cassandra-broker.mydomain/com", clientFactory);

    private final CreateServiceInstanceRequest request = aCreateServiceInstanceRequest();

    private Request aFeignRequest = Request.create(Request.HttpMethod.GET, "https://url.domain", Collections.emptyMap(),
        Request.Body.empty(), new RequestTemplate());


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
        Plan plan1 = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan21 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition1 =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan1, plan21)).build();
        Catalog catalog = Catalog.builder().serviceDefinitions(serviceDefinition1).build();

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("keyspace_name", "foo");
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .parameters(parameters)
                .serviceInstanceId("service-instance-id")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .parameters(parameters)
                .apiInfoLocation("api-location")
                .platformInstanceId("cf-instance-id")
                .originatingIdentity(aContext())
                .build();
        CreateServiceInstanceRequest mappedRequest = osbProxy.mapProvisionRequest(request, catalog);

        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getPlatformInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
        assertThat(mappedRequest.getParameters()).isEqualTo(parameters);
    }

    @Test
    public void maps_bind_request_to_1st_service_and_1st_plan() {
        Plan plan1 = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan21 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition1 =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan1, plan21)).build();
        Catalog catalog = Catalog.builder().serviceDefinitions(serviceDefinition1).build();


        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = BindResource.builder()
                .appGuid("app_guid")
                .route(null)
                .properties(routeBindingParams)
                .build();

        Context cfContext = CloudFoundryContext.builder()
                .organizationGuid("org_guid")
                .spaceGuid("space_guid")
                .build();

        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId("coab-serviceid")
                .planId("plan_id")
                .bindResource(bindResource)
                .context(cfContext)
                .parameters(serviceBindingParams)
                .bindingId("service-instance-binding-id")
                .serviceInstanceId("service-instance-id")
                .apiInfoLocation("api-info")
                .originatingIdentity(aContext())
                .platformInstanceId("cf-instance-id")
                .serviceInstanceId("service-instance-id")
                .build();

        CreateServiceInstanceBindingRequest mappedRequest = osbProxy.mapBindRequest(request, catalog);

        assertThat(mappedRequest.getBindingId()).isEqualTo("service-instance-binding-id");
        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
        assertThat(mappedRequest.getParameters()).isEqualTo(serviceBindingParams);
    }

    @Test
    public void maps_deprovision_request_to_1st_service_and_1st_plan() {
        Plan plan1 = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan21 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition1 =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan1, plan21)).build();
        Catalog catalog = Catalog.builder().serviceDefinitions(serviceDefinition1).build();

        // Given an incoming delete request
        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
                .serviceInstanceId("instance_id")
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(true)
                .apiInfoLocation("api-location")
                .platformInstanceId("cf-instance-id")
                .originatingIdentity(aContext())
                .build();

        DeleteServiceInstanceRequest mappedRequest = osbProxy.mapDeprovisionRequest(request, catalog);

        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("instance_id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getPlatformInstanceId()).isEqualTo("cf-instance-id");
        assertThat(mappedRequest.getOriginatingIdentity()).isEqualTo(aContext());
    }

    @Test
    public void maps_unbind_request_to_1st_service_and_1st_plan() {
        Plan plan1 = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan21 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition1 =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan1, plan21)).build();
        Catalog catalog = Catalog.builder().serviceDefinitions(serviceDefinition1).build();

        // Given an incoming delete request
        DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
                .serviceInstanceId("instance_id")
                .bindingId("service-binding-id")
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceDefinition(ServiceDefinition.builder().build())
                .apiInfoLocation("api-location")
                .platformInstanceId("cf-instance-id")
                .originatingIdentity(aContext())
                .build();

        DeleteServiceInstanceBindingRequest mappedRequest = osbProxy.mapUnbindRequest(request, catalog);

        assertThat(mappedRequest.getBindingId()).isEqualTo("service-binding-id");
        assertThat(mappedRequest.getServiceDefinitionId()).isEqualTo("service_id");
        assertThat(mappedRequest.getPlanId()).isEqualTo("plan_id");
        assertThat(mappedRequest.getServiceInstanceId()).isEqualTo("instance_id");
        assertThat(mappedRequest.getApiInfoLocation()).isEqualTo("api-location");
        assertThat(mappedRequest.getPlatformInstanceId()).isEqualTo("cf-instance-id");
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
        //Given a parameter request
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .serviceInstanceId("service-instance-guid")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .asyncAccepted(true)
                .apiInfoLocation("api-info")
                .originatingIdentity(aContext())
                .build();

        ServiceInstanceServiceClient serviceInstanceServiceClient = mock(ServiceInstanceServiceClient.class);
        osbProxy.delegateProvision(request, serviceInstanceServiceClient);

        verify(serviceInstanceServiceClient).createServiceInstance(
                "service-instance-guid",
                false, //for now OsbProxyImpl expects sync broker response
                "api-info",
                aContextOriginatingHeader(),
                OsbConstants.X_Broker_API_Version_Value,
                request);
    }

    @Test
    public void delegates_bind_call() {
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = BindResource.builder()
                .appGuid("app_guid")
                .route(null)
                .properties(routeBindingParams)
                .build();

        Context cfContext = CloudFoundryContext.builder()
                .organizationGuid("org_guid")
                .spaceGuid("space_guid")
                .build();

        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .bindResource(bindResource)
                .context(cfContext)
                .parameters(serviceBindingParams)
                .bindingId("service-instance-binding-id")
                .serviceInstanceId("service-instance-id")
                .apiInfoLocation("api-info")
                .originatingIdentity(aContext())
                .platformInstanceId("cf-instance-id")
                .serviceInstanceId("service-instance-id")
                .build();

        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = mock(ServiceInstanceBindingServiceClient.class);
        //noinspection unused
        ResponseEntity<CreateServiceInstanceAppBindingResponse> responseEntity = osbProxy.delegateBind(request, serviceInstanceBindingServiceClient);

        verify(serviceInstanceBindingServiceClient).createServiceInstanceBinding(
                "service-instance-id",
                "service-instance-binding-id",
                false,
                "api-info",
                aContextOriginatingHeader(),
                OsbConstants.X_Broker_API_Version_Value,
                request);
    }


    @Test
    public void delegates_deprovision_call() {

        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
                .serviceInstanceId("service-instance-id")
                .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(true)
                .apiInfoLocation("api-info")
                .platformInstanceId("cf-instance-id")
                .originatingIdentity(aContext())
                .build();

        ServiceInstanceServiceClient serviceInstanceServiceClient = mock(ServiceInstanceServiceClient.class);
        @SuppressWarnings("unused") ResponseEntity<DeleteServiceInstanceResponse> responseEntity = osbProxy.delegateDeprovision(request, serviceInstanceServiceClient);

        verify(serviceInstanceServiceClient).deleteServiceInstance(
                "service-instance-id",
                "coab-serviceid",
                "coab-planid",
                false,
                "api-info",
                aContextOriginatingHeader(),
                OsbConstants.X_Broker_API_Version_Value);
    }

    @Test
    public void delegates_unbind_call() {
        DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
                .serviceInstanceId("service-instance-id")
                .bindingId("service-binding-id")
                .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .serviceDefinition(ServiceDefinition.builder().build())
                .apiInfoLocation("api-info")
                .platformInstanceId("cf-instance-id")
                .originatingIdentity(aContext())
                .build();

        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = mock(ServiceInstanceBindingServiceClient.class);
        osbProxy.delegateUnbind(request, serviceInstanceBindingServiceClient);

        verify(serviceInstanceBindingServiceClient).deleteServiceInstanceBinding(
                "service-instance-id",
                "service-binding-id",
                "coab-serviceid",
                "coab-planid",
                false,
                "api-info",
                aContextOriginatingHeader(),
                OsbConstants.X_Broker_API_Version_Value);
    }



    @Test
    public void maps_successull_deprovision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        DeleteServiceInstanceResponse deleteServiceInstanceResponse = DeleteServiceInstanceResponse.builder()
                .async(false).build();
        DeleteServiceInstanceResponse delegatedResponse = DeleteServiceInstanceResponse.builder()
                .async(false).build();
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
    public void maps_rejected_deprovision_to_success() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        Response errorResponse = Response.builder()
                .status(HttpStatus.GONE.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"No such service instance 1234\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#deleteServiceInstance(String,String,String,boolean,String,String)", errorResponse);

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapDeprovisionResponse(originalResponse, null, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.SUCCEEDED);
        assertThat(mappedResponse.getDescription()).isNull();
        assertThat(mappedResponse.isDeleteOperation()).isTrue();
    }

    /**
     * Brokers that never received service instance requests (e.g. due to timeout, or internal bugs) should not prevent the
     * bosh deployment from being removed
     */
    @Test
    public void maps_rejected_deprovision_bis_to_success() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        Response errorResponse = Response.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Cannot drop non existing keyspace 'ks86f715e3_9450_4faf_9255_9bceb158375f'.\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#deleteServiceInstance(String,String,String,boolean,String,String)", errorResponse);

        //when
        GetLastServiceOperationResponse mappedResponse = osbProxy.mapDeprovisionResponse(originalResponse, null, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.SUCCEEDED);
        assertThat(mappedResponse.getDescription()).isNull();
        assertThat(mappedResponse.isDeleteOperation()).isTrue();
    }



    @Test
    public void maps_successfull_provision_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        CreateServiceInstanceResponse delegatedResponse = CreateServiceInstanceResponse.builder()
                .async(false)
                .dashboardUrl("https://a-inner-dashboard.com").build();
        ResponseEntity<CreateServiceInstanceResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.CREATED);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") GetLastServiceOperationResponse mappedResponse = osbProxy.mapProvisionResponse(originalResponse, delegatedResponseEnveloppe, provisionException, aCatalog());

        assertThat(mappedResponse.getState()).isSameAs(OperationState.SUCCEEDED);
        assertThat(mappedResponse.getDescription()).isNull();
        assertThat(mappedResponse.isDeleteOperation()).isFalse();
        assertThat(mappedResponse.isDeleteOperation()).isFalse();
    }

    @Test
    public void maps_successull_bind_response() {
        //Given
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        CreateServiceInstanceAppBindingResponse delegatedResponse = CreateServiceInstanceAppBindingResponse.builder()
                .credentials(credentials)
                .bindingExisted(false)
                .build();

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
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        CreateServiceInstanceAppBindingResponse delegatedResponse = CreateServiceInstanceAppBindingResponse.builder()
                .credentials(credentials)
                .bindingExisted(true)
                .build();

        ResponseEntity<CreateServiceInstanceAppBindingResponse> delegatedResponseEnveloppe
                = new ResponseEntity<>(delegatedResponse, HttpStatus.OK);
        FeignException provisionException = null;

        //when
        @SuppressWarnings("ConstantConditions") CreateServiceInstanceAppBindingResponse mappedResponse = osbProxy.mapBindResponse(delegatedResponseEnveloppe, provisionException, aCatalog());


        assertThat(mappedResponse.isBindingExisted()).isTrue();
        assertThat(mappedResponse).isEqualTo(delegatedResponse);
    }

    @Test
    public void maps_rejected_provision_request() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        Response errorReponse = Response.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        //when
        //noinspection unused
        Exception exception = assertThrows(Exception.class,
            () -> osbProxy.mapProvisionResponse(originalResponse, null, provisionException,
                aCatalog()));
        assertThat(exception.getMessage()).contains("Missing required fields: keyspace param");
    }

    @Test
    public void maps_rejected_bind_request() {
        //Given
        Response errorReponse = Response.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceBindingServiceClient#createServiceInstanceBinding(String,String,String,String,CreateServiceInstanceBindingRequest)", errorReponse);

        //when

        Exception exception = assertThrows(Exception.class,
            () -> osbProxy.mapBindResponse(null, provisionException, aCatalog()));
        assertThat(exception.getMessage()).contains("Missing required fields: keyspace param");
    }

    @Test
    public void maps_feign_client_unknown_errors_to_their_original_value() {
        //Given
        Response errorReponse = Response.builder()
                .status(HttpStatus.INSUFFICIENT_STORAGE.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        //when
        ResponseStatusException mapClientException = osbProxy.mapClientException(provisionException);

        assertThat(mapClientException.getReason()).isEqualTo("Missing required fields: keyspace param");
        assertThat(mapClientException.getRawStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE.value());
    }

    @Test
    public void maps_feign_client_known_errors() {
        assertDescriptionAndStatusMapped(HttpStatus.BAD_REQUEST, "Missing required fields: keyspace param");
        assertDescriptionAndStatusMapped(HttpStatus.CONFLICT, "Service instance with id 1234 already exists");
        assertDescriptionAndStatusMapped(HttpStatus.GONE, null);
        //should better really map this to error when we get more time.
        assertDescriptionAndStatusMapped(HttpStatus.UNPROCESSABLE_ENTITY, "This Service Plan requires client support for asynchronous service operations.");
    }

    private void assertDescriptionAndStatusMapped(HttpStatus httpStatus, String description) {
        //given
        Response.Builder builder = Response.builder()
            .status(httpStatus.value())
            .headers(new HashMap<>());
        if (description != null) {
            builder.body("{\"description\":\"" + description + "\"}", Charset.defaultCharset());
        }
        Response errorResponse = builder
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorResponse);

        //when
        ResponseStatusException mapClientException = osbProxy.mapClientException(provisionException);

        //then
        assertThat(mapClientException.getReason()).isEqualTo(description);
        assertThat(mapClientException.getStatus()).isEqualTo(httpStatus);
    }

    @Test
    public void parses_error_response_description_body() {
        Response errorReponse = Response.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .headers(new HashMap<>())
                .body("{\"description\":\"Missing required fields: keyspace param\"}", Charset.defaultCharset())
                .request(aFeignRequest)
                .build();
        FeignException provisionException = FeignException.errorStatus("ServiceInstanceServiceClient#createServiceInstance(String,boolean,String,String,CreateServiceInstanceRequest)", errorReponse);

        OsbProxyImpl.ErrorMessage errorMessage = osbProxy.parseReponseBody(provisionException);

        assertThat(errorMessage.getDescription()).isEqualTo("Missing required fields: keyspace param");
    }

    @Test
    public void parses_osb_client_exceptions_into_generic_message() {
        //Given
        DecodeException decodeException = new DecodeException(500, "Could not extract response: no suitable " +
            "HttpMessageConverter found for response type [?] and content type [text/plain;charset=UTF-8]",
            aFeignRequest);

        //when
        String description = osbProxy.parseReponseBody(decodeException).getDescription();

        assertThat(description).isEqualTo("Internal error, please contact administrator");
    }

    @Test
    public void maps_async_creation_response() {
        //Given
        GetLastServiceOperationResponse originalResponse = aPreviousOnGoingOperation();
        CreateServiceInstanceResponse delegatedResponse = CreateServiceInstanceResponse.builder()
                .async(false)
                .dashboardUrl("https://a-inner-dashboard.com").build();
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
        return OsbBuilderHelper.aCatalog();
    }

    private Context aContext() {
        return OsbBuilderHelper.aCfUserContext();
    }

    private GetLastServiceOperationResponse aPreviousOnGoingOperation() {
        return GetLastServiceOperationResponse.builder()
                    .operationState(OperationState.IN_PROGRESS)
                    .description(null)
                .build();
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        return CreateServiceInstanceRequest.builder()
        .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .context(OsbBuilderHelper.aCfUserContext())
                .serviceInstanceId("service-instance-id")
                .build();
    }

}
