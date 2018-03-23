package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModuleHelper;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer.NO_OP_INITIALIZER;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.*;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.CassandraBrokerApplication.SECRETS_REPOSITORY_ALIAS_NAME;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;


/**
 * Will detect all components present in classpath, including CassandraBrokerApplication
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CassandraServiceProvisionningTest {

    private static final String SERVICE_DEFINITION_ID = "cassandra-ondemand-service";
    private static final String SERVICE_PLAN_ID = "cassandra-ondemand-plan";
    /**
     * Define an environment variable to turn on wiremock recording.
     * Set the url of the broker forward requests to (e.g. "https://cassandra-broker.mydomain.com"
     *
     * More background in http://wiremock.org/docs/record-playback/
     */
    @Value("${preprodBrokerUrlToRecord:}")
    private String preprodBrokerUrlToRecord;
    @Value("${preprodBrokerUser:}")
    private String preprodBrokerUser;
    @Value("${preprodBrokerPassword:}")
    private String preprodBrokerPassword;

    private static final String SERVICE_INSTANCE_ID = "111";
    private static final String SERVICE_BINDING_INSTANCE_ID = "222";
    @LocalServerPort
    int port;
    private GitServer gitServer;

    @Autowired
    OsbProxyImpl osbProxy;

    @Autowired
    @Qualifier(value = "secretsGitProcessor")
    GitProcessor secretsGitProcessor;

    @Autowired
    OsbProxyProperties osbProxyProperties;

    @Autowired
    DeploymentProperties deploymentProperties;
    @Autowired
    OsbClientFactory clientFactory;

    private CatalogServiceClient catalogServiceClient;
    private ServiceInstanceBindingServiceClient serviceInstanceBindingService;
    private ServiceInstanceServiceClient serviceInstanceService;


    private boolean isWiremockRecordingEnabled() {
        return preprodBrokerUrlToRecord != null && ! preprodBrokerUrlToRecord.isEmpty();
    }


    @Before
    public void startHttpClient() {
        RestAssured.port = port;
        RestAssured.authentication = basic("user", "secret");
    }

    @Before
    public void initializeOsbClientsToLocalSystemUnderTest() {
        String url = "http://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";

        //when
        catalogServiceClient = clientFactory.getClient(url, user, password, CatalogServiceClient.class);
        serviceInstanceService = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);
        serviceInstanceBindingService = clientFactory.getClient(url, user, password, ServiceInstanceBindingServiceClient.class);
    }
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8088)
            .httpsPort(8089)
            .notifier(new Slf4jNotifier(true))
    );



    @Before
    public void setUpWireMockRecording() {
        if (isWiremockRecordingEnabled()) {
            WireMock.startRecording(preprodBrokerUrlToRecord);
            assertThat(preprodBrokerPassword).isNotEmpty();
            assertThat(preprodBrokerUser).isNotEmpty();
            osbProxy.setOsbDelegatePassword(preprodBrokerPassword);
            osbProxy.setOsbDelegateUser(preprodBrokerUser);
        }
    }

    @After
    public void stopWireMockRecording() {
        if (isWiremockRecordingEnabled()) {
            @SuppressWarnings("unused") SnapshotRecordResult recordedMappings = WireMock.stopRecording();
            // No need to try to print resulting SnapshotRecordResult as wiremock debug traces already do so.
            // When empty, when wiremock indeed received some requests to record
        }
    }


    @Before
    public void startGitServer() throws IOException, GitAPIException {
        gitServer = new GitServer();

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
            Path coabDepls = gitWorkDir.toPath().resolve(deploymentProperties.getRootDeployment());
            //sub deployments
            Path templateDir = coabDepls
                    .resolve(deploymentProperties.getModelDeployment())
                    .resolve(deploymentProperties.getTemplate());
            createDir(templateDir);
            createDummyFile(templateDir.resolve(deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION));
            createDummyFile(templateDir.resolve(deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + deploymentProperties.getVars()+ DeploymentConstants.YML_EXTENSION));

            Path operatorsDir = coabDepls
                    .resolve(deploymentProperties.getModelDeployment())
                    .resolve(deploymentProperties.getOperators());
            createDir(operatorsDir);
            createDummyFile(operatorsDir.resolve(DeploymentConstants.COAB + DeploymentConstants.HYPHEN + deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION));

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
            Path coabDepls = gitWorkDir.toPath().resolve(deploymentProperties.getRootDeployment());
            //sub deployments
            Path secretsDir = coabDepls
                    .resolve(deploymentProperties.getModelDeployment())
                    .resolve(deploymentProperties.getSecrets());
            createDir(secretsDir);
            createDummyFile(secretsDir.resolve(deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION));
            createDummyFile(secretsDir.resolve(deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION));
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
        @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), osbProxyProperties.getMaxExecutionDurationSeconds(), Mockito.mock(OsbProxy.class));
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
        exposes_catalog();
        // not yet ready
//        String operation = create_async_service_instance_using_osb_client();
        String operation = create_async_service_instance_using_rest_assured();

        polls_last_operation(operation, HttpStatus.SC_OK, "in progress", "");

        simulateManifestGeneration(secretsGitProcessor);

        polls_last_operation(operation, HttpStatus.SC_OK, "succeeded", "");

        create_service_binding();
        delete_service_binding();

        String deleteOperation = delete_a_service_instance();

        polls_last_operation(deleteOperation,
                HttpStatus.SC_GONE, //async delete expects a 410 status
                "succeeded", "succeeded");
    }

    private void create_service_binding() {
        CreateServiceInstanceBindingRequest serviceInstanceBindingRequest = aBindingRequest(SERVICE_INSTANCE_ID)
                .withBindingId(SERVICE_BINDING_INSTANCE_ID);

        @SuppressWarnings("unchecked") ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingService.createServiceInstanceBinding(
                SERVICE_INSTANCE_ID,
                SERVICE_BINDING_INSTANCE_ID,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aContext()),
                serviceInstanceBindingRequest);
        assertThat(bindResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(bindResponse.getBody()).isNotNull();
        Map<String, Object> credentials = bindResponse.getBody().getCredentials();
        assertThat(credentials).isNotNull();
    }

    private void delete_service_binding() {
        serviceInstanceBindingService.deleteServiceInstanceBinding(
                SERVICE_INSTANCE_ID,
                SERVICE_BINDING_INSTANCE_ID,
                SERVICE_DEFINITION_ID,
                SERVICE_PLAN_ID,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aContext()));
    }

    private String create_async_service_instance_using_osb_client() {

        CreateServiceInstanceRequest createServiceInstanceRequest = aCreateServiceInstanceRequest()
                .withServiceInstanceId(SERVICE_INSTANCE_ID);
        @SuppressWarnings("unchecked") ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceService.createServiceInstance(
                SERVICE_INSTANCE_ID,
                true,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aContext()),
                createServiceInstanceRequest);
        assertThat(createResponse.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(createResponse.getBody()).isNotNull();
        return createResponse.getBody().getOperation();
    }

    public String create_async_service_instance_using_rest_assured() {

        @SuppressWarnings("UnnecessaryLocalVariable")
        String operation = given()
                .basePath("/v2")
                .contentType("application/json")
                .body(aCreateServiceInstanceRequest()).
                when()
                .put("/service_instances/{id}", SERVICE_INSTANCE_ID).
                then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body("operation", not(isEmptyString()))
                .extract().
                    path("operation");

        return operation;
    }

    @Test
    public void exposes_catalog() {
        Catalog catalog = catalogServiceClient.getCatalog();
        assertThat(catalog.getServiceDefinitions()).isNotEmpty();
        Assertions.assertThat(catalog).isNotNull();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        Assertions.assertThat(serviceDefinition).isNotNull();
        Plan defaultPlan = serviceDefinition.getPlans().get(0);
        Assertions.assertThat(defaultPlan).isNotNull();
    }

    public void polls_last_operation(final String operation, int expectedStatusCode, String firstExpectedKeyword, String secondExpectedKeyword) {

        given()
                .basePath("/v2")
                .contentType("application/json")
                .param("operation", operation)
                .param("plan_id", SERVICE_PLAN_ID)
                .param("service_id", SERVICE_DEFINITION_ID).
                when()
                .get("/service_instances/{id}/last_operation", SERVICE_INSTANCE_ID).
                then()
                .statusCode(expectedStatusCode)
                .body(containsString(firstExpectedKeyword))
                .body(containsString(secondExpectedKeyword)); //hard coded start date way in the past
    }

    public String delete_a_service_instance() {

        @SuppressWarnings("UnnecessaryLocalVariable")
        String operation = given()
                .basePath("/v2")
                .param("service_id", SERVICE_DEFINITION_ID)
                .param("plan_id", SERVICE_PLAN_ID)
                .param("accepts_incomplete", true).
                        when()
                .delete("/service_instances/{id}", SERVICE_INSTANCE_ID).
                        then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body("operation", not(isEmptyString()))
                .extract().
                        path("operation");

        return operation;
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
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID,
                SERVICE_PLAN_ID,
                "org_id",
                "space_id",
                createServiceInstanceContext,
                params
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        return request;
    }

}