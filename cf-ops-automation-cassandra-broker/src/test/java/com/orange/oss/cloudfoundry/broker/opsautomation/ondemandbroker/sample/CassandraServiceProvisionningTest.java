package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.CassandraProcessorConstants;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineCompletionTracker;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer.NO_OP_INITIALIZER;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.CassandraBrokerApplication.SECRETS_REPOSITORY_ALIAS_NAME;
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

    private Clock clock = Clock.fixed(Instant.now(), ZoneId.of("Europe/Paris"));
    private static final String SERVICE_INSTANCE_ID = "111";
    @LocalServerPort
    int port;
    private GitServer gitServer;

    @Autowired
    @Qualifier(value = "secretsGitProcessor")
    GitProcessor secretsGitProcessor;

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
        gitServer.startEphemeralReposServer(NO_OP_INITIALIZER);
        gitServer.initRepo("paas-template.git", this::initPaasTemplate);
        gitServer.initRepo("paas-secrets.git", this::initPaasSecret);
    }

    public void initPaasTemplate(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();

            //In develop branch
            git.checkout().setName("develop").setCreateBranch(true).call();

            //root deployment
            Path coabDepls = gitWorkDir.toPath().resolve(CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            //sub deployments
            Path templateDir = coabDepls
                    .resolve(CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY)
                    .resolve(CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            createDir(templateDir);
            createDummyFile(templateDir.resolve(CassandraProcessorConstants.MODEL_MANIFEST_FILENAME));
            createDummyFile(templateDir.resolve(CassandraProcessorConstants.MODEL_VARS_FILENAME));

            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

// potentially submodule public template extract
//            git.submoduleInit().call();
//            git.submoduleAdd().setPath("bosh-deployment").setURI(GIT_BASE_URL + "bosh-deployment.git").call();
//            git.submoduleAdd().setPath("mysql-deployment").setURI(GIT_BASE_URL + "mysql-deployment").call();
            git.commit().setMessage("CassandraServiceProvisionningTest#initPaasTemplate").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initPaasSecret(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();

            //root deployment
            Path coabDepls = gitWorkDir.toPath().resolve(CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            //sub deployments
            Path templateDir = coabDepls
                    .resolve(CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY)
                    .resolve(CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            createDir(templateDir);

            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

// potentially submodule public template extract
//            git.submoduleInit().call();
//            git.submoduleAdd().setPath("bosh-deployment").setURI(GIT_BASE_URL + "bosh-deployment.git").call();
//            git.submoduleAdd().setPath("mysql-deployment").setURI(GIT_BASE_URL + "mysql-deployment").call();
            git.commit().setMessage("CassandraServiceProvisionningTest#initPaasSecret").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void simulateManifestGeneration(GitProcessor gitProcessor) throws IOException {
        Context context = new Context();
        gitProcessor.preCreate(context);

        Path workDirPath = (Path) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString());
        @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), Mockito.mock(OsbProxy.class));
        Path targetManifestFilePath = tracker.getTargetManifestFilePath(workDirPath, SERVICE_INSTANCE_ID);
        createDir(targetManifestFilePath.getParent());
        createDummyFile(targetManifestFilePath);

        gitProcessor.postCreate(context);
    }

    public static void createDir(Path dir) throws IOException {
        Files.createDirectories(dir);
        //TODO: create .gitignore
        try (Writer writer = new FileWriter(dir.resolve(".gitkeep").toFile())) {
            writer.write("Please keep me");
        }
    }

    public static void createDummyFile(Path path) throws IOException {
        try (Writer writer = new FileWriter(path.toFile())) {
            writer.write("dummy content");
        }
    }


    @After
    public void stopGitServer() throws InterruptedException {
        gitServer.stopAndCleanupReposServer();
    }

    @Test
    public void supports_crud_lifecycle() throws IOException {
        create_async_service_instance();
        @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(clock, Mockito.mock(OsbProxy.class));
        String jsonPipelineOperationState = tracker.getPipelineOperationStateAsJson(aCreateServiceInstanceRequest());

        polls_last_operation(jsonPipelineOperationState, HttpStatus.SC_OK, "in progress", "Creation is in progress");


        simulateManifestGeneration(secretsGitProcessor);

        polls_last_operation(jsonPipelineOperationState, HttpStatus.SC_OK, "succeeded", "Creation is succeeded");

//        delete_a_service_instance();
//        polls_last_operation("delete", 410, "succeeded", "succeeded");
    }


    public void create_async_service_instance() {


        given()
                .basePath("/v2")
                .contentType("application/json")
                .body(aCreateServiceInstanceRequest()).
                when()
                .put("/service_instances/{id}", SERVICE_INSTANCE_ID).
                then()
                .statusCode(HttpStatus.SC_ACCEPTED);


    }

    public void polls_last_operation(final String operation, int expectedStatusCode, String firstExpectedKeyword, String secondExpectedKeyword) {

        given()
                .basePath("/v2")
                .contentType("application/json")
                .param("operation", operation)
                .param("plan_id", "cassandra-ondemand-plan")
                .param("service_id", "cassandra-ondemand-service").
                when()
                .get("/service_instances/{id}/last_operation", SERVICE_INSTANCE_ID).
                then()
                .statusCode(expectedStatusCode)
                .body(containsString(firstExpectedKeyword))
                .body(containsString(secondExpectedKeyword)); //hard coded start date way in the past
    }

    public void delete_a_service_instance() {

        given()
                .basePath("/v2")
                .param("service_id", "cassandra-ondemand-service")
                .param("plan_id", "cassandra-ondemand-plan")
                .param("accepts_incomplete", true).
                when()
                .delete("/service_instances/{id}", SERVICE_INSTANCE_ID).
                then()
                .statusCode(HttpStatus.SC_ACCEPTED);

    }


    public static File getFileFromClasspath(String tfStateFileInClasspath) {
        String path = TerraformModuleHelper.class.getResource(tfStateFileInClasspath).getFile();
        File tfStateFile = new File(path);
        assertThat(tfStateFile).exists();
        return tfStateFile;
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {

        Map<String, Object> params = new HashMap<>();

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
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        return request;
    }

}