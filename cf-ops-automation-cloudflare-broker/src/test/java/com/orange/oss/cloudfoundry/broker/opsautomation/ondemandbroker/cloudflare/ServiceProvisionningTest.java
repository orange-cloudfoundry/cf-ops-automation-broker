package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.cloudflare;


import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
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
    public void supports_crud_lifecycle() {
        create_async_service_instance();
        polls_last_create_operation();
        delete_a_service_instance();
    }


    public void create_async_service_instance() {

        Map<String, Object> params = new HashMap<>();
        params.put("route", "a-valid-route");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("cloudflare-route",
                "cloudflare-default",
                "org_id",
                "space_id",
                params
                );

        given()
                .basePath("/v2")
                .contentType("application/json")
                .body(request).
        when()
                .put("/service_instances/{id}", "111").
        then()
                .statusCode(HttpStatus.SC_ACCEPTED);


    }

    public void polls_last_create_operation() {

        given()
                .basePath("/v2")
                .contentType("application/json")
                .param("operation", "2017-11-14T17:24:08.007Z")
                .param("plan_id", "cloudflare-default")
                .param("service_id", "cloudflare-route").
        when()
                .get("/service_instances/{id}/last_operation", "111").
        then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("failed"))
                .body(containsString("timeout")); //hard coded start date way in the past
    }

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