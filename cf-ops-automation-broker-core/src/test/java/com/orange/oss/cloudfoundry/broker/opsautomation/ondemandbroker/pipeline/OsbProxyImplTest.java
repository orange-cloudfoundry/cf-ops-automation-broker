package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.*;

import java.util.Collections;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        String url = osbProxy.getBrokerUrl(request);
        assertThat(url).isEqualTo("https://service-instance-id-cassandra-broker.mydomain/com");
    }
    @Test
    public void constructs_osb_client() {
        CatalogServiceClient catalogServiceClient = osbProxy.constructCatalogClient("https://service-instance-id-cassandra-broker.mydomain/com");
        ServiceInstanceServiceClient serviceInstanceServiceClient = osbProxy.constructServiceInstanceServiceClient("https://service-instance-id-cassandra-broker.mydomain/com");
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
    public void delegates_call() {

    }

    @Test
    public void maps_response() {

    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("coab-serviceid", "coab-planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        return request;
    }

}