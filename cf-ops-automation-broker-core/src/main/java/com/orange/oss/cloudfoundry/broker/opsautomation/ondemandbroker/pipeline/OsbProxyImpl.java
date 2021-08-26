package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class OsbProxyImpl implements OsbProxy {

    private String osbDelegateUser;
    private String osbDelegatePassword;
    private String brokerUrlPattern;
    private OsbClientFactory clientFactory;
    private Gson gson;
    private ObjectMapper objectMapper;
    private static Logger logger = LoggerFactory.getLogger(OsbProxyImpl.class.getName());

    private static final String FEIGN_MESSAGE_SEPARATOR = "]: [";

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
    public GetLastServiceOperationResponse delegateProvision(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        String brokerUrl = getBrokerUrl(pollingRequest.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog(OsbConstants.X_Broker_API_Version_Value);
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

    @Override
    public CreateServiceInstanceBindingResponse delegateBind(CreateServiceInstanceBindingRequest request) {
        String brokerUrl = getBrokerUrl(request.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog(OsbConstants.X_Broker_API_Version_Value);
        CreateServiceInstanceBindingRequest mappedRequest = mapBindRequest(request, catalog);
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = constructServiceInstanceBindingServiceClient(brokerUrl);

        ResponseEntity<CreateServiceInstanceAppBindingResponse> delegatedResponse;
        FeignException bindException;
        try {
            bindException = null;
            delegatedResponse = delegateBind(mappedRequest, serviceInstanceBindingServiceClient);
        } catch (FeignException e) {
            bindException = e;
            delegatedResponse = null;
        }

        //noinspection UnnecessaryLocalVariable
        CreateServiceInstanceBindingResponse mappedResponse = mapBindResponse(delegatedResponse, bindException, catalog);
        return mappedResponse;
    }

    @Override
    public GetLastServiceOperationResponse delegateDeprovision(GetLastServiceOperationRequest pollingRequest, DeleteServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        String brokerUrl = getBrokerUrl(pollingRequest.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog(OsbConstants.X_Broker_API_Version_Value);
        DeleteServiceInstanceRequest mappedRequest = mapDeprovisionRequest(request, catalog);
        ServiceInstanceServiceClient serviceInstanceServiceClient = constructServiceInstanceServiceClient(brokerUrl);

        FeignException provisionException = null;
        ResponseEntity<DeleteServiceInstanceResponse> delegatedResponse = null;
        try {
            delegatedResponse = delegateDeprovision(mappedRequest, serviceInstanceServiceClient);
        } catch (FeignException e) {
            provisionException = e;
        }
        //noinspection UnnecessaryLocalVariable
        GetLastServiceOperationResponse mappedResponse = mapDeprovisionResponse(response, delegatedResponse, provisionException, catalog);
        return mappedResponse;
    }

    @Override
    public void delegateUnbind(DeleteServiceInstanceBindingRequest request) {
        String brokerUrl = getBrokerUrl(request.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog(OsbConstants.X_Broker_API_Version_Value);
        DeleteServiceInstanceBindingRequest mappedRequest = mapUnbindRequest(request, catalog);
        ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient = constructServiceInstanceBindingServiceClient(brokerUrl);

        try {
            delegateUnbind(mappedRequest, serviceInstanceBindingServiceClient);
        } catch (FeignException e) {
            logger.warn("inner broker bind request rejected:" + e);
            throw mapClientException(e);
        }

    }



    GetLastServiceOperationResponse mapProvisionResponse(GetLastServiceOperationResponse response, ResponseEntity<CreateServiceInstanceResponse> delegatedResponse, FeignException provisionException, Catalog catalog) {
        OperationState operationState;
        String description = null;
        if (provisionException != null) {
            throw mapClientException(provisionException);
        } else {
            if (delegatedResponse.getStatusCode() == HttpStatus.CREATED) {
                operationState = OperationState.SUCCEEDED;
            } else {
                logger.error("Unexpected inner broker response with code {} only synchronous provisionning supported for now. Full response follows: {}", delegatedResponse.getStatusCode(), delegatedResponse);
                operationState = OperationState.FAILED;
                description = "Internal error, please contact administrator";
            }
        }
        return GetLastServiceOperationResponse.builder()
                .operationState(operationState)
                .description(description)
                .deleteOperation(false)
                .build();
    }

    CreateServiceInstanceAppBindingResponse mapBindResponse(ResponseEntity<CreateServiceInstanceAppBindingResponse> delegatedResponse, FeignException bindException, Catalog catalog) {
        if (bindException != null) {
            logger.warn("inner broker bind request rejected:" + bindException);
            throw mapClientException(bindException);
        }
        CreateServiceInstanceAppBindingResponse delegatedResponseBody = delegatedResponse.getBody();
        return CreateServiceInstanceAppBindingResponse.builder()
                .bindingExisted(delegatedResponse.getStatusCode() == HttpStatus.OK)
                .credentials(delegatedResponseBody.getCredentials())
                .syslogDrainUrl(delegatedResponseBody.getSyslogDrainUrl())
                .volumeMounts(delegatedResponseBody.getVolumeMounts())
                .build();
    }

    ResponseStatusException mapClientException(FeignException exception) {
        //Note: we don't pass the FeignException as cause to not leak confidential internal details such as the url
        // available in the FeignException
        ErrorMessage errorMessage = parseReponseBody(exception);
        String description = errorMessage == null ? null: errorMessage.getDescription();
        return new ResponseStatusException(exception.status(), description, null);
    }


    GetLastServiceOperationResponse mapDeprovisionResponse(GetLastServiceOperationResponse originalResponse, ResponseEntity<DeleteServiceInstanceResponse> delegatedResponse, FeignException deprovisionException, Catalog catalog) {
        OperationState operationState;
        String description = null;
        if (deprovisionException != null) {
            operationState = OperationState.SUCCEEDED;
            logger.warn("Ignoring rejected inner broker deprovision request:" + deprovisionException);
        } else {
            if (delegatedResponse.getStatusCode() == HttpStatus.OK) {
                operationState = OperationState.SUCCEEDED;
            } else {
                logger.error("Unexpected inner broker response with code {} only synchronous deprovisionning supported for now. Full response follows: {}", delegatedResponse.getStatusCode(), delegatedResponse);
                operationState = OperationState.FAILED;
                description = "Internal error, please contact administrator";
            }
        }
        return GetLastServiceOperationResponse.builder()
                .operationState(operationState)
                .description(description)
                .deleteOperation(true)
                .build();
    }

    ResponseEntity<CreateServiceInstanceResponse> delegateProvision(CreateServiceInstanceRequest request, ServiceInstanceServiceClient serviceInstanceServiceClient) {
        return serviceInstanceServiceClient.createServiceInstance(
                request.getServiceInstanceId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()),
                OsbConstants.X_Broker_API_Version_Value,
                request);
    }

    ResponseEntity<DeleteServiceInstanceResponse> delegateDeprovision(DeleteServiceInstanceRequest request, ServiceInstanceServiceClient serviceInstanceServiceClient) {
        return serviceInstanceServiceClient.deleteServiceInstance(
                request.getServiceInstanceId(),
                request.getServiceDefinitionId(),
                request.getPlanId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()),
                OsbConstants.X_Broker_API_Version_Value);
    }

    void delegateUnbind(DeleteServiceInstanceBindingRequest request, ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient) {
        serviceInstanceBindingServiceClient.deleteServiceInstanceBinding(
                request.getServiceInstanceId(),
                request.getBindingId(),
                request.getServiceDefinitionId(),
                request.getPlanId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()),
                OsbConstants.X_Broker_API_Version_Value);
    }

    ResponseEntity<CreateServiceInstanceAppBindingResponse> delegateBind(CreateServiceInstanceBindingRequest request, ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient) {
        return serviceInstanceBindingServiceClient.createServiceInstanceBinding(
                request.getServiceInstanceId(),
                request.getBindingId(),
                false,
                request.getApiInfoLocation(),
                buildOriginatingIdentityHeader(request.getOriginatingIdentity()),
                OsbConstants.X_Broker_API_Version_Value,
                request);
    }

    /**
     * Inspired from spring-cloud-open-service-broker, see https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/c56080e5ec8ed97ba8fe4e15ac2031073fbc45ae/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/servlet/ControllerIntegrationTest.java#L36
     */
    public String buildOriginatingIdentityHeader(Context originatingIdentity) {
        if (originatingIdentity == null) {
            return null;
        }
        String platform = originatingIdentity.getPlatform();

        Map<String, Object> propMap = new HashMap<>();
        if (OsbConstants.ORIGINATING_CLOUDFOUNDRY_PLATFORM.equals(platform)) {
            Object userKey = originatingIdentity.getProperty(OsbConstants.ORIGINATING_USER_KEY);
            if (userKey != null) {
                propMap.put(OsbConstants.ORIGINATING_USER_KEY, userKey);
            }
            Object email = originatingIdentity.getProperty(OsbConstants.ORIGINATING_EMAIL_KEY);
            if (email != null) {
                propMap.put(OsbConstants.ORIGINATING_EMAIL_KEY, email);
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

        return CreateServiceInstanceRequest.builder()
                .serviceDefinitionId(mappedService.getId())
                .planId(mappedPlan.getId())
                .context(r.getContext())
                .originatingIdentity(r.getOriginatingIdentity())
                .parameters(mappedParameters)
                .serviceInstanceId(r.getServiceInstanceId())
                .platformInstanceId(r.getPlatformInstanceId())
                .apiInfoLocation(r.getApiInfoLocation())
                .build();
    }

    CreateServiceInstanceBindingRequest mapBindRequest(CreateServiceInstanceBindingRequest r, Catalog catalog) {
        ServiceDefinition mappedService = catalog.getServiceDefinitions().get(0);
        Plan mappedPlan = mappedService.getPlans().get(0);
        Map<String, Object> mappedParameters = r.getParameters();

        return CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId(mappedService.getId())
                .planId(mappedPlan.getId())
                .bindResource(r.getBindResource())
                .context(r.getContext())
                .parameters(mappedParameters)
                .bindingId(r.getBindingId())
                .serviceInstanceId(r.getServiceInstanceId())
                .platformInstanceId(r.getPlatformInstanceId())
                .apiInfoLocation(r.getApiInfoLocation())
                .originatingIdentity(r.getOriginatingIdentity())
                .build();
    }

    DeleteServiceInstanceRequest mapDeprovisionRequest(DeleteServiceInstanceRequest r, Catalog catalog) {
        ServiceDefinition mappedService = catalog.getServiceDefinitions().get(0);
        Plan mappedPlan = mappedService.getPlans().get(0);

        return DeleteServiceInstanceRequest.builder()
                .serviceInstanceId(r.getServiceInstanceId())
                .serviceDefinitionId(mappedService.getId())
                .planId(mappedPlan.getId())
                .serviceDefinition(mappedService)
                .asyncAccepted(false)
                .platformInstanceId(r.getPlatformInstanceId())
                .apiInfoLocation(r.getApiInfoLocation())
                .originatingIdentity(r.getOriginatingIdentity())
                .build();
    }

    DeleteServiceInstanceBindingRequest mapUnbindRequest(DeleteServiceInstanceBindingRequest r, Catalog catalog) {
        ServiceDefinition mappedService = catalog.getServiceDefinitions().get(0);
        Plan mappedPlan = mappedService.getPlans().get(0);

        return DeleteServiceInstanceBindingRequest.builder()
                .serviceInstanceId(r.getServiceInstanceId())
                .bindingId(r.getBindingId())
                .serviceDefinitionId(mappedService.getId())
                .planId(mappedPlan.getId())
                .serviceDefinition(mappedService)
                .platformInstanceId(r.getPlatformInstanceId())
                .apiInfoLocation(r.getApiInfoLocation())
                .originatingIdentity(r.getOriginatingIdentity())
                .build();
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

    ServiceInstanceBindingServiceClient constructServiceInstanceBindingServiceClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, ServiceInstanceBindingServiceClient.class);
    }

    ErrorMessage parseReponseBody(FeignException provisionException)  {
        String exceptionMessage = provisionException.getMessage();
        int jsonStart = exceptionMessage.lastIndexOf(FEIGN_MESSAGE_SEPARATOR);
        if (jsonStart != -1) {
            //found the delimiter, trim it
            jsonStart += FEIGN_MESSAGE_SEPARATOR.length();
        }
        ErrorMessage errorMessage;
        if (jsonStart == -1) {
            logger.info("unable to find nested user-facing description json string within: " + exceptionMessage + ".  using raw exception message as descr");
            errorMessage = new ErrorMessage("Internal error, please contact administrator");
        } else {
            String json = exceptionMessage.substring(jsonStart);
            json = json.substring(0, json.length()-1);
            errorMessage = gson.fromJson(json, ErrorMessage.class);
        }
        return errorMessage;
    }

    /**
     * POJO for parsing error response. Workaround to the fact that spring-cloud-cloudfoundry-service-broker is lacking public noop contructor necessary for jackson to deserialize it
     * https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/c56080e5ec8ed97ba8fe4e15ac2031073fbc45ae/spring-cloud-open-service-broker/src/main/java/org/springframework/cloud/servicebroker/model/ErrorMessage.java#L28
     */
    static class ErrorMessage {
        private final String description;

        ErrorMessage(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

    }

}
