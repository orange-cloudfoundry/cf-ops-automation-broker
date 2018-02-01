package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Test;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
//import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;

import java.text.MessageFormat;
import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

/**
 * - construct OSB client: construct url from serviceInstanceId, and configured static pwd
 * - fetch catalog
 * - map req
 * - provisionning instance
 * - map resp
 */
public class OsbProxyImplTest {

    OsbProxy<CreateServiceInstanceRequest> osbProxy = new OsbProxyImpl<>("user", "password");
    GetLastServiceOperationRequest pollingRequest;
    private CreateServiceInstanceRequest request = aCreateServiceInstanceRequest();
    GetLastServiceOperationResponse response;
// OsbClientFactory clientFactory;


    @Test
    public void constructs_osb_client() {

        String serviceInstanceId = request.getServiceInstanceId();
        String brokerUrlPattern = "https://{0}-cassandra-broker.mydomain/com";
        String url = MessageFormat.format(brokerUrlPattern, serviceInstanceId);

        assertThat(url).isEqualTo("https://service-instance-id-cassandra-broker.mydomain/com");

        //clientFactory.
    }

    @Test
    public void maps_request() {

    }

    @Test
    public void delegates_call() {

    }

    @Test
    public void maps_response() {

    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("serviceid", "planid", "orgguid", "spaceguid", new HashMap<>());
        request.withServiceInstanceId("service-instance-id");
        return request;
    }

}