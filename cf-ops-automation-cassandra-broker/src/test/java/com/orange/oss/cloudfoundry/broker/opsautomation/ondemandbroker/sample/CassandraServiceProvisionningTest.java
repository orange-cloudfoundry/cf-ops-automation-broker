package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModuleHelper;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

/**
 * Will detect all components present in classpath, including CassandraBrokerApplication
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CassandraServiceProvisionningTest {

    @LocalServerPort
    int port;
    GitServer gitServer;

    @Before
    public void startHttpClient() {
        RestAssured.port = port;
        RestAssured.authentication = basic("user", "secret");
    }


    @Before
    public void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();

        Consumer<Git> initPaasSecret = git -> {
            File gitWorkDir = git.getRepository().getDirectory().getParentFile();
            try {
                AddCommand addC = git.add().addFilepattern(".");
                addC.call();
                git.commit().setMessage("CassandraServiceProvisionningTest#startGitServer").call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        gitServer.startEphemeralReposServer(initPaasSecret);
    }


    @After
    public void stopGitServer() throws InterruptedException {
        gitServer.stopAndCleanupReposServer();
    }

    @Test
    public void supports_crud_lifecycle() {
        create_async_service_instance();
        polls_last_operation("create", HttpStatus.SC_OK, "failed", "timeout");
        delete_a_service_instance();
        polls_last_operation("delete", 410, "succeeded", "succeeded");
    }


    public void create_async_service_instance() {

        Map<String, Object> params = new HashMap<>();
        params.put("route-prefix", "a-valid-route");

        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id");
        org.springframework.cloud.servicebroker.model.Context createServiceInstanceContext = new org.springframework.cloud.servicebroker.model.Context(
                CLOUD_FOUNDRY_PLATFORM,
                contextProperties
        );
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("cassandra-ondemand-service",
                "cassandra-ondemand-plan",
                "org_id",
                "space_id",
                createServiceInstanceContext,
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

    public void polls_last_operation(final String operation, int expectedStatusCode, String firstExpectedKeyword, String secondExpectedKeyword) {

        given()
                .basePath("/v2")
                .contentType("application/json")
                .param("operation", "{\"lastOperationDate\":\"2017-11-14T17:24:08.007Z\",\"operation\":\"" + operation + "\"}")
                .param("plan_id", "cloudflare-default")
                .param("service_id", "cloudflare-route").
                when()
                .get("/service_instances/{id}/last_operation", "111").
                then()
                .statusCode(expectedStatusCode)
                .body(containsString(firstExpectedKeyword))
                .body(containsString(secondExpectedKeyword)); //hard coded start date way in the past
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
                .statusCode(HttpStatus.SC_ACCEPTED);

    }


    public static File getFileFromClasspath(String tfStateFileInClasspath) {
        String path = TerraformModuleHelper.class.getResource(tfStateFileInClasspath).getFile();
        File tfStateFile = new File(path);
        assertThat(tfStateFile).exists();
        return tfStateFile;
    }


}