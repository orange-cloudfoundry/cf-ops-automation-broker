package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.SimpleGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentConstants;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.SecretsGenerator;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Copy;
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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer.NO_OP_INITIALIZER;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingRequest;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aCfUserContext;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.BoshBrokerApplication.SECRETS_REPOSITORY_ALIAS_NAME;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;


/**
 * Will detect all components present in classpath, including BoshBrokerApplication
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class BoshServiceProvisionningTest {

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
    SimpleGitManager secretsSimpleGitManager;

    @Autowired
    OsbProxyProperties osbProxyProperties;

    @Autowired
    DeploymentProperties deploymentProperties;

    @Autowired
    SecretsGenerator secretsGenerator;

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

            //Search for the sample-deployment
            Path referenceDataModel = Paths.get("../sample-deployment");

            //Copy reference data model
            EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            Copy.TreeCopier tc = new Copy.TreeCopier(referenceDataModel, gitWorkDir.toPath(), "coab-depls", false, true);
            Files.walkFileTree(referenceDataModel, opts, Integer.MAX_VALUE, tc);




            //sub deployments
/*            Path templateDir = coabDepls
                    .resolve(deploymentProperties.getModelDeployment())
                    .resolve(DeploymentConstants.TEMPLATE);
            createDir(templateDir);
            createDummyFile(templateDir.resolve(deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION));
            createDummyFile(templateDir.resolve(deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + DeploymentConstants.VARS + DeploymentConstants.YML_EXTENSION));

            Path operatorsDir = coabDepls
                    .resolve(deploymentProperties.getModelDeployment())
                    .resolve(DeploymentConstants.OPERATORS);
            createDir(operatorsDir);
            createDummyFile(operatorsDir.resolve(DeploymentConstants.COAB + DeploymentConstants.HYPHEN + DeploymentConstants.OPERATORS + DeploymentConstants.YML_EXTENSION));
*/
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

// potentially submodule public template extract
//            git.submoduleInit().call();
//            git.submoduleAdd().setPath("bosh-deployment").setURI(GIT_BASE_URL + "bosh-deployment.git").call();
//            git.submoduleAdd().setPath("mysql-deployment").setURI(GIT_BASE_URL + "mysql-deployment").call();
            git.commit().setMessage("BoshServiceProvisionningTest#initPaasTemplate").call();

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
                    .resolve(DeploymentConstants.SECRETS);
            createDir(secretsDir);
            createDummyFile(secretsDir.resolve(DeploymentConstants.META + DeploymentConstants.YML_EXTENSION));
            createDummyFile(secretsDir.resolve(DeploymentConstants.SECRETS + DeploymentConstants.YML_EXTENSION));
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

// potentially submodule public template extract
//            git.submoduleInit().call();
//            git.submoduleAdd().setPath("bosh-deployment").setURI(GIT_BASE_URL + "bosh-deployment.git").call();
//            git.submoduleAdd().setPath("mysql-deployment").setURI(GIT_BASE_URL + "mysql-deployment").call();
            git.commit().setMessage("BoshServiceProvisionningTest#initPaasSecret").call();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void simulateManifestGeneration(SimpleGitManager simpleGitManager) throws IOException {
        Context context = new Context();
        simpleGitManager.preCreate(context);

        Path workDirPath = (Path) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString());
        Path targetManifestFilePath = secretsGenerator.getTargetManifestFilePath(workDirPath, SERVICE_INSTANCE_ID);
        createDir(targetManifestFilePath.getParent());
        createDummyFile(targetManifestFilePath);

        simpleGitManager.postCreate(context);
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

        String operation = create_async_service_instance_using_osb_client();

        polls_last_operation(operation, HttpStatus.SC_OK, "in progress", "");

        simulateManifestGeneration(secretsSimpleGitManager);

        polls_last_operation(operation, HttpStatus.SC_OK, "succeeded", "");

        create_service_binding();
        delete_service_binding();

        String deleteOperation = delete_a_service_instance_using_osb_client();

        polls_last_operation(deleteOperation,
                HttpStatus.SC_GONE, //async delete expects a 410 status
                "succeeded", "succeeded");
    }

    private void create_service_binding() {
        CreateServiceInstanceBindingRequest serviceInstanceBindingRequest = aBindingRequest(SERVICE_INSTANCE_ID)
                .withBindingId(SERVICE_BINDING_INSTANCE_ID);

        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingService.createServiceInstanceBinding(
                SERVICE_INSTANCE_ID,
                SERVICE_BINDING_INSTANCE_ID,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
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
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()));
    }

    private String create_async_service_instance_using_osb_client() {

        CreateServiceInstanceRequest createServiceInstanceRequest = aCreateServiceInstanceRequest()
                .withServiceInstanceId(SERVICE_INSTANCE_ID);
        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceService.createServiceInstance(
                SERVICE_INSTANCE_ID,
                true,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
                createServiceInstanceRequest);
        assertThat(createResponse.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(createResponse.getBody()).isNotNull();
        return createResponse.getBody().getOperation();
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

    private String delete_a_service_instance_using_osb_client() {

        ResponseEntity<DeleteServiceInstanceResponse> response = serviceInstanceService.deleteServiceInstance(
                SERVICE_INSTANCE_ID,
                SERVICE_DEFINITION_ID,
                SERVICE_PLAN_ID,
                true,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()));
        assertThat(response.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getOperation();
    }



    public static File getFileFromClasspath(String tfStateFileInClasspath) {
        String path = TerraformModuleHelper.class.getResource(tfStateFileInClasspath).getFile();
        File tfStateFile = new File(path);
        assertThat(tfStateFile).exists();
        return tfStateFile;
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID,
                SERVICE_PLAN_ID,
                "org_id",
                "space_id",
                aCfOsbContext(),
                new HashMap<>()
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        return request;
    }

    private org.springframework.cloud.servicebroker.model.Context aCfOsbContext() {
        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id");
        return new org.springframework.cloud.servicebroker.model.Context(
                CLOUD_FOUNDRY_PLATFORM,
                contextProperties
        );
    }

}