package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Base64Utils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class OsbProxyImpl<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> implements OsbProxy<Q> {

    private String osbDelegateUser;
    private String osbDelegatePassword;
    private String brokerUrlPattern;
    private OsbClientFactory clientFactory;
    private Gson gson;
    private ObjectMapper objectMapper;
    private static Logger logger = LoggerFactory.getLogger(OsbProxyImpl.class.getName());

    private static final String FEIGN_MESSAGE_SEPARATOR = "content:\n";

    public OsbProxyImpl(String osbDelegateUser, String osbDelegatePassword, String brokerUrlPattern, OsbClientFactory clientFactory) {
        this.osbDelegateUser = osbDelegateUser;
        this.osbDelegatePassword = osbDelegatePassword;
        this.brokerUrlPattern = brokerUrlPattern;
        this.clientFactory = clientFactory;
        objectMapper = Jackson2ObjectMapperBuilder.json().build();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
    }

    /**
     * Used by tests to inject password from environment variable without leaking credentials in test properties files
     */
    public void setOsbDelegatePassword(String osbDelegatePassword) {
        this.osbDelegatePassword = osbDelegatePassword;
    }

    /**
     * Used by tests to inject login from environment variable without leaking credentials in test properties files
     */
    public void setOsbDelegateUser(String osbDelegateUser) {
        this.osbDelegateUser = osbDelegateUser;
    }

    @Override
    public GetLastServiceOperationResponse delegate(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        String brokerUrl = getBrokerUrl(pollingRequest.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog();
        CreateServiceInstanceRequest mappedRequest = mapProvisionRequest(request, catalog);
        ServiceInstanceServiceClient serviceInstanceServiceClient = constructServiceInstanceServiceClient(brokerUrl);
        
        ResponseEntity<CreateServiceInstanceResponse> delegatedResponse = null;
        FeignException provisionException = null;
        try {
            delegatedResponse = delegateProvision(mappedRequest, serviceInstanceServiceClient);
        } catch (FeignException e) {
            provisionException = e;
        }
        //noinspection UnnecessaryLocalVariable
        GetLastServiceOperationResponse mappedResponse = mapProvisionResponse(response, delegatedResponse, provisionException, catalog);
        return mappedResponse;
    }



    GetLastServiceOperationResponse mapProvisionResponse(GetLastServiceOperationResponse response, ResponseEntity<CreateServiceInstanceResponse> delegatedResponse, FeignException provisionException, Catalog catalog) {
        OperationState operationState;
        String description = null;
        if (provisionException != null) {
            operationState = OperationState.FAILED;
            description = parseReponseBody(provisionException.getMessage()).getDescription();
        } else {
            if (delegatedResponse.getStatusCode() == HttpStatus.CREATED) {
                operationState = OperationState.SUCCEEDED;
            } else {
                logger.error("Unexpected inner broker response with code {} only synchronous provisionning supported for now. Full response follows: {}", delegatedResponse.getStatusCode(), delegatedResponse);
                operationState = OperationState.FAILED;
                description = "Internal error, please contact administrator";
            }
        }
        return new GetLastServiceOperationResponse()
                .withOperationState(operationState)
                .withDescription(description);
    }


    GetLastServiceOperationResponse mapDeprovisionResponse(GetLastServiceOperationResponse originalResponse, ResponseEntity<DeleteServiceInstanceResponse> delegatedResponse, FeignException deprovisionException, Catalog catalog) {
        OperationState operationState;
        String description = null;
        if (deprovisionException != null) {
            operationState = OperationState.FAILED;
            description = parseReponseBody(deprovisionException.getMessage()).getDescription();
        } else {
            if (delegatedResponse.getStatusCode() == HttpStatus.OK) {
                operationState = OperationState.SUCCEEDED;
            } else {
                logger.error("Unexpected inner broker response with code {} only synchronous deprovisionning supported for now. Full response follows: {}", delegatedResponse.getStatusCode(), delegatedResponse);
                operationState = OperationState.FAILED;
                description = "Internal error, please contact administrator";
            }
        }
        return new GetLastServiceOperationResponse()
                .withOperationState(operationState)
                .withDescription(description);
    }

    ResponseEntity<CreateServiceInstanceResponse> delegateProvision(CreateServiceInstanceRequest request, ServiceInstanceServiceClient serviceInstanceServiceClient) {
        //noinspection unchecked
        return (ResponseEntity<CreateServiceInstanceResponse>) serviceInstanceServiceClient.createServiceInstance(
                request.getServiceInstanceId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()),
                request);
    }

    void delegateDeprovision(DeleteServiceInstanceRequest request, ServiceInstanceServiceClient serviceInstanceServiceClient) {
        //noinspection unchecked
        serviceInstanceServiceClient.deleteServiceInstance(
                request.getServiceInstanceId(),
                request.getServiceDefinitionId(),
                request.getPlanId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()));
    }
    static final String ORIGINATING_USER_KEY = "user_id";

    static final String ORIGINATING_EMAIL_KEY = "email";

    static final String ORIGINATING_CLOUDFOUNDRY_PLATFORM = "cloudfoundry";

    /**
     * Inspired from spring-cloud-open-service-broker, see https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/c56080e5ec8ed97ba8fe4e15ac2031073fbc45ae/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/servlet/ControllerIntegrationTest.java#L36
     */
    String buildOriginatingIdentityHeader(Context originatingIdentity) {
        if (originatingIdentity == null) {
            return null;
        }
        String platform = originatingIdentity.getPlatform();

        Map<String, Object> propMap = new HashMap<>();
        if (ORIGINATING_CLOUDFOUNDRY_PLATFORM.equals(platform)) {
            Object userKey = originatingIdentity.getProperty(ORIGINATING_USER_KEY);
            if (userKey != null) {
                propMap.put(ORIGINATING_USER_KEY, userKey);
            }
            Object email = originatingIdentity.getProperty(ORIGINATING_EMAIL_KEY);
            if (email != null) {
                propMap.put(ORIGINATING_EMAIL_KEY, email);
            }
        }
        //Wait for next spring-cloud-open-service-broker version which defines a getProperties() method
        // to access all properties, see
        // https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/90e5cd2b9ae5dcf639836a1367079822d5f8a5a9/spring-cloud-open-service-broker/src/main/java/org/springframework/cloud/servicebroker/model/Context.java#L69

        String properties;
        try {
            properties = objectMapper.writeValueAsString(propMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.toString(), e);
        }
        String encodedProperties = new String(Base64Utils.encode(properties.getBytes()));
        return platform+ " " + encodedProperties;
    }


    CreateServiceInstanceRequest mapProvisionRequest(CreateServiceInstanceRequest r, Catalog catalog) {
        ServiceDefinition mappedService = catalog.getServiceDefinitions().get(0);
        Plan mappedPlan = mappedService.getPlans().get(0);
        Map<String, Object> mappedParameters = r.getParameters();

        //noinspection deprecation
        CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
                mappedService.getId(),
                mappedPlan.getId(),
                r.getOrganizationGuid(),
                r.getSpaceGuid(),
                r.getContext(),
                mappedParameters);
        createServiceInstanceRequest.withServiceInstanceId(r.getServiceInstanceId());
        createServiceInstanceRequest.withCfInstanceId(r.getCfInstanceId());
        createServiceInstanceRequest.withApiInfoLocation(r.getApiInfoLocation());
        createServiceInstanceRequest.withOriginatingIdentity(r.getOriginatingIdentity());
        return createServiceInstanceRequest;
    }
    String getBrokerUrl(String serviceInstanceId) {
        return MessageFormat.format(this.brokerUrlPattern, serviceInstanceId);
    }

    CatalogServiceClient constructCatalogClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, CatalogServiceClient.class);
    }

    ServiceInstanceServiceClient constructServiceInstanceServiceClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, ServiceInstanceServiceClient.class);
    }

    ErrorMessage parseReponseBody(@SuppressWarnings("SameParameterValue") String json)  {
        int jsonStart = json.lastIndexOf(FEIGN_MESSAGE_SEPARATOR);
        if (jsonStart != -1) {
            //found the delimiter, trim it
            jsonStart += FEIGN_MESSAGE_SEPARATOR.length();
        } else {
            //try another weaker delimiter in case feign lib changed behavior in between
            jsonStart = json.indexOf("{");
        }
        if (jsonStart == -1) {
            throw new RuntimeException("unable to find nested json string within: " + json);
        }
        json = json.substring(jsonStart);
        return gson.fromJson(json, ErrorMessage.class);
    }

    /**
     * POJO for parsing error response. Workaround to the fact that spring-cloud-cloudfoundry-service-broker is lacking public noop contructor necessary for jackson to deserialize it
     * https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/c56080e5ec8ed97ba8fe4e15ac2031073fbc45ae/spring-cloud-open-service-broker/src/main/java/org/springframework/cloud/servicebroker/model/ErrorMessage.java#L28
     */
    static class ErrorMessage {
        private final String description;

        public ErrorMessage(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

    }

}
