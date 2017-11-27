package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ServiceProvisionningTest {

    @LocalServerPort
    int port;

    @Before
    public void setup() {
        RestAssured.port = port;
        RestAssured.authentication = basic("user", "secret");
    }

    @Test
    public void create_a_service_instance() {

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("ondemand-service",
                "ondemand-plan",
                "org_id",
                "space_id", new HashMap<>());

        given()
                .basePath("/v2")
                .contentType("application/json")
                .body(request).
        when()
                .put("/service_instances/{id}", "111").
        then()
                .statusCode(HttpStatus.SC_CREATED);

    }

    @Test
    public void delete_a_service_instance() {

        given()
                .basePath("/v2")
                .param("service_id", "ondemand-service")
                .param("plan_id", "ondemand-plan")
                .param("accepts_incomplete", true).
        when()
                .delete("/service_instances/{id}", "111").
        then()
                .statusCode(HttpStatus.SC_OK);

    }


}