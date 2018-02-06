package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import org.springframework.cloud.servicebroker.model.*;

import java.text.MessageFormat;
import java.util.Map;

public class OsbProxyImpl<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> implements OsbProxy<Q> {
    private final String osbDelegateUser;
    private final String osbDelegatePassword;
    private String brokerUrlPattern;
    private OsbClientFactory clientFactory;

    public OsbProxyImpl(String osbDelegateUser, String osbDelegatePassword, String brokerUrlPattern, OsbClientFactory clientFactory) {
        this.osbDelegateUser = osbDelegateUser;
        this.osbDelegatePassword = osbDelegatePassword;
        this.brokerUrlPattern = brokerUrlPattern;
        this.clientFactory = clientFactory;
    }

    @Override
    public GetLastServiceOperationResponse delegate(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        String brokerUrl = getBrokerUrl(pollingRequest.getServiceInstanceId());
        CatalogServiceClient catalogServiceClient = constructCatalogClient(brokerUrl);
        Catalog catalog = catalogServiceClient.getCatalog();
        CreateServiceInstanceRequest mappedRequest = mapRequest(request, catalog);
        ServiceInstanceServiceClient serviceInstanceServiceClient = constructServiceInstanceServiceClient(brokerUrl);
        return response;
    }

    CreateServiceInstanceRequest mapRequest(CreateServiceInstanceRequest r, Catalog catalog) {
        ServiceDefinition mappedService = catalog.getServiceDefinitions().get(0);
        Plan mappedPlan = mappedService.getPlans().get(0);
        Map<String, Object> mappedParameters = r.getParameters();

        //noinspection deprecation
        return new CreateServiceInstanceRequest(
                mappedService.getId(),
                mappedPlan.getId(),
                r.getOrganizationGuid(),
                r.getSpaceGuid(),
                r.getContext(),
                mappedParameters);
    }

    String getBrokerUrl(String serviceInstanceId) {
        this.brokerUrlPattern = "https://{0}-cassandra-broker.mydomain/com";
        String brokerUrlPattern = this.brokerUrlPattern;
        return MessageFormat.format(brokerUrlPattern, serviceInstanceId);
    }

    CatalogServiceClient constructCatalogClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, CatalogServiceClient.class);
    }
    ServiceInstanceServiceClient constructServiceInstanceServiceClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, ServiceInstanceServiceClient.class);
    }
}
