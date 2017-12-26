package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class OsbClientTestApplicationTest {

    @LocalServerPort
    int port;

    @Autowired
    CatalogServiceClient catalogServiceClient;
    @Autowired
    ServiceInstanceServiceClient serviceInstanceServiceClient;
    @Autowired
    ServiceInstanceBindingServiceClient serviceInstanceBindingServiceClient;


    @Test
    @Ignore("Need to configure dynamically host in feign client")
    public void invokes_catalog() {
        assertThat(catalogServiceClient.getCatalog()).isNotNull();
    }
}
