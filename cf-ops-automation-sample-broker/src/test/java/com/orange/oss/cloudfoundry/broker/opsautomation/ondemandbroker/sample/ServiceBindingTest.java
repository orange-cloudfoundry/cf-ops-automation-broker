package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ServiceBindingTest {

    @LocalServerPort
    int port;

    @Before
    public void setup() {
        RestAssured.port = port;
        RestAssured.authentication = basic("user", "secret");
    }

    @Test
    public void bind_a_service_instance() {

        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest("ondemand-service",
                "ondemand-plan",
                "app_id",
                new HashMap<>());

        given()
                .basePath("/v2")
                .contentType("application/json")
                .body(request).
        when()
                .put("/service_instances/{instance_id}/service_bindings/{binding_id}", "111", "222").
        then()
                .statusCode(HttpStatus.SC_CREATED);

    }

    @Test
    public void unbind_a_service_instance() {

        given()
                .basePath("/v2")
                .param("service_id", "ondemand-service")
                .param("plan_id", "ondemand-plan").
        when()
                .delete("/service_instances/{instance_id}/service_bindings/{binding_id}", "111", "222").
        then()
                .statusCode(HttpStatus.SC_OK);

    }


}