package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceBindingServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.ServiceInstanceServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.BoshDeploymentManifestDTO;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.CoabVarsFileDto;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.CoabVarsFileDtoBuilder;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentConstants;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.ReadOnlyServiceInstanceBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.SecretsGenerator;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Copy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import feign.FeignException;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer.NO_OP_INITIALIZER;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.MEDIUM_SERVICE_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.SERVICE_DEFINITION_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.SERVICE_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.UPGRADED_SERVICE_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aBindingRequest;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.aCfUserContext;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.BoshBrokerApplication.SECRETS_REPOSITORY_ALIAS_NAME;
import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;


/**
 * Will detect all components present in classpath, including BoshBrokerApplication
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, classes =
    {BoshBrokerApplication.class, WireMockTestConfiguration.class})
@TestExecutionListeners(value = BoshServiceProvisionningTest.HermeticGitServerTestListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class BoshServiceProvisionningTest {

	private static final Logger logger = LoggerFactory.getLogger(BoshServiceProvisionningTest.class.getName());
    public static final String BROKERED_SERVICE_INSTANCE_ID = "brokered_service_instance_id";

    /**
     * Git repo needs to be started before the spring context gets loaded and RetrierGitManager bean to be instanciated.
     * More details in docs 05-eager-git-clones.md
     */
    public static class HermeticGitServerTestListener implements TestExecutionListener {

        GitServer gitServer;

        @Override
        public void beforeTestClass(TestContext testContext) throws Exception {
            DeploymentProperties deploymentProperties = new DeploymentProperties();
            //TODO: find a better way to the deployment name consistent with application.properties
            //- trying to access spring context from TestContext triggers the GitRetrier bean initialization which
            // fails as git server isn't yet loaded
            //- load the properties file from the classloader into a Properties object and extract the property needed
            deploymentProperties.setModelDeployment("mongodb");
            gitServer(deploymentProperties);
        }

        @Override
        public void afterTestClass(TestContext testContext) throws Exception {
            gitServer.stopAndCleanupReposServer();
        }

        public GitServer gitServer(DeploymentProperties deploymentProperties) throws IOException {
            gitServer = new GitServer();

            gitServer.startEphemeralReposServer(NO_OP_INITIALIZER);
            gitServer.initRepo("paas-template.git", git -> initPaasTemplate(git, deploymentProperties));
            gitServer.initRepo("paas-secrets.git", git -> initPaasSecret(git, deploymentProperties));

            return gitServer;
        }
    }

    @BeforeAll
    public static void prepare_CONFIG_YML_env_var() throws Exception {

        InputStream resourceAsStream = BoshServiceProvisionningTest.class.getResourceAsStream("/catalog.yml");
        assertThat(resourceAsStream)
                .describedAs("expecting catalog.yml in classpath")
                .isNotNull();
        try (Reader dataFileReader = new InputStreamReader(resourceAsStream)) {
            String CATALOG_YML = IOUtils.toString(dataFileReader);
            System.setProperty("CATALOG_YML", CATALOG_YML);
            assertThat(System.getProperty("CATALOG_YML")).isNotEmpty();
        }
    }

    @AfterEach
    public void after() {
        System.clearProperty("CATALOG_YML");
        assertThat(System.getProperty("CATALOG_YML")).isNull();
    }



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

    @Autowired
    OsbProxyImpl osbProxy;

    @Autowired
    @Qualifier(value = "secretsGitProcessor")
    GitProcessor gitSecretsProcessor;

    @Autowired
    @Qualifier(value = "templateGitProcessor")
    GitProcessor gitTemplatesProcessor;

    @Autowired
    @Qualifier(value = "secretsGitManager")
    GitManager secretsGitManager;

    @Autowired
    @Qualifier(value = "templatesGitManager")
    GitManager templatesGitManager;

    @Autowired
    @Qualifier("templateGitProperties")
    GitProperties templatesGitProperties;

    @Autowired
    @Qualifier("secretsGitProperties")
    GitProperties secretsGitProperties;

    @Autowired
    OsbProxyProperties osbProxyProperties;

    @Autowired
    DeploymentProperties deploymentProperties;

    @Autowired
    SecretsGenerator secretsGenerator;

    @Autowired
    OsbClientFactory clientFactory;

    @Autowired
    ReadOnlyServiceInstanceBrokerProcessor readOnlyServiceInstanceBrokerProcessor;

    private CatalogServiceClient catalogServiceClient;
    private ServiceInstanceBindingServiceClient serviceInstanceBindingService;
    private ServiceInstanceServiceClient serviceInstanceService;


    private boolean isWiremockRecordingEnabled() {
        return preprodBrokerUrlToRecord != null && ! preprodBrokerUrlToRecord.isEmpty();
    }


    @BeforeEach
    public void startHttpClient() {
        RestAssured.port = port;
        RestAssured.authentication = basic("user", "secret");
    }

    @BeforeEach
    public void initializeOsbClientsToLocalSystemUnderTest() {
        String url = "http://127.0.0.1:" + port;
        String user = "user";
        String password = "secret";

        //when
        catalogServiceClient = clientFactory.getClient(url, user, password, CatalogServiceClient.class);
        serviceInstanceService = clientFactory.getClient(url, user, password, ServiceInstanceServiceClient.class);
        serviceInstanceBindingService = clientFactory.getClient(url, user, password, ServiceInstanceBindingServiceClient.class);
    }

    @BeforeEach
    public void setUpWireMockRecording() {
        if (isWiremockRecordingEnabled()) {
            WireMock.startRecording(preprodBrokerUrlToRecord);
            assertThat(preprodBrokerPassword).isNotEmpty();
            assertThat(preprodBrokerUser).isNotEmpty();
            osbProxy.setOsbDelegatePassword(preprodBrokerPassword);
            osbProxy.setOsbDelegateUser(preprodBrokerUser);
        }
    }

    @AfterEach
    public void stopWireMockRecording() {
        if (isWiremockRecordingEnabled()) {
            @SuppressWarnings("unused") SnapshotRecordResult recordedMappings = WireMock.stopRecording();
            // No need to try to print resulting SnapshotRecordResult as wiremock debug traces already do so.
            // When empty, when wiremock indeed received some requests to record
        }
    }


    public static void initPaasTemplate(Git git, DeploymentProperties deploymentProperties) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();

            //In develop branch
            git.checkout().setName("develop").setCreateBranch(true).call();

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

    public static void initPaasSecret(Git git, DeploymentProperties deploymentProperties) {
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
            createBoshManifestFile(secretsDir.resolve(DeploymentConstants.META + DeploymentConstants.YML_EXTENSION), null);
            createBoshManifestFile(secretsDir.resolve(DeploymentConstants.SECRETS + DeploymentConstants.YML_EXTENSION), null);
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

    /**
     * Simultate manifest generated upon successful service instance provisioning.
     * White box injection of manifest: does not protect against git pooling bugs
     */
    public void simulateProvisionManifestGeneration(GitProcessor gitProcessor,
        CreateServiceInstanceRequest createServiceInstanceRequest) throws IOException {
        CoabVarsFileDtoBuilder coabVarsFileDtoBuilder = new CoabVarsFileDtoBuilder();
        String deploymentName =
            deploymentProperties.getModelDeploymentShortAlias() +
                deploymentProperties.getModelDeploymentSeparator() +
                createServiceInstanceRequest.getServiceInstanceId();
        CoabVarsFileDto coabVarsFileDto =
            coabVarsFileDtoBuilder.wrapCreateOsbIntoVarsDto(createServiceInstanceRequest, deploymentName);


        simulateManifestGeneration(gitProcessor, coabVarsFileDto);
    }

    /**
     * Simultate manifest generated upon successful service instance update.
     * White box injection of manifest: does not protect against git pooling bugs
     */
    public void simulateUpdatingManifestGeneration(GitProcessor gitProcessor,
        UpdateServiceInstanceRequest updateServiceInstanceRequest) throws IOException {

        CoabVarsFileDtoBuilder coabVarsFileDtoBuilder = new CoabVarsFileDtoBuilder();
        String deploymentName =
            deploymentProperties.getModelDeploymentShortAlias() +
                deploymentProperties.getModelDeploymentSeparator() +
                updateServiceInstanceRequest.getServiceInstanceId();
        CoabVarsFileDto coabVarsFileDto =
            coabVarsFileDtoBuilder.wrapUpdateOsbIntoVarsDto(updateServiceInstanceRequest, deploymentName);

        simulateManifestGeneration(gitProcessor, coabVarsFileDto);
    }

    private void simulateManifestGeneration(GitProcessor gitProcessor, CoabVarsFileDto coabVarsFileDto)
        throws IOException {
        Context context = new Context();
        gitProcessor.preCreate(context);

        Path workDirPath = (Path) context.contextKeys
            .get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir);
        Path targetManifestFilePath = secretsGenerator.getTargetManifestFilePath(workDirPath, SERVICE_INSTANCE_ID);
        createDir(targetManifestFilePath.getParent());
        createBoshManifestFile(targetManifestFilePath, coabVarsFileDto);

        gitProcessor.postCreate(context);
        gitProcessor.cleanUp(context); //make sure to return to the pool
    }

    public static void createDir(Path dir) throws IOException {
        Files.createDirectories(dir);
        //TODO: create .gitignore
        try (Writer writer = new FileWriter(dir.resolve(".gitkeep").toFile())) {
            writer.write("Please keep me");
        }
    }

    public static void createBoshManifestFile(Path path, CoabVarsFileDto coabVarsFileDto) throws IOException {
        BoshDeploymentManifestDTO boshDeploymentManifestDTO = new BoshDeploymentManifestDTO();
        boshDeploymentManifestDTO.coabCompletionMarker=coabVarsFileDto;
        try (FileWriter manifestWriter = new FileWriter(path.toFile())) {
            ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
            manifestWriter.write(ymlMapper.writeValueAsString(boshDeploymentManifestDTO));
        }
    }


    @Test
    public void supports_crud_lifecycle() throws IOException {
        exposes_catalog();

        String operation = create_async_service_instance_using_osb_client();

        polls_last_operation(operation, HttpStatus.SC_OK, "in progress", "");

        simulateProvisionManifestGeneration(gitSecretsProcessor, aCreateServiceInstanceRequest());

        polls_last_operation(operation, HttpStatus.SC_OK, "succeeded", "");

        create_service_binding();

        operation = update_service_plan(anAcceptedPlanUpdateServiceInstanceRequest());
        polls_last_operation(operation, HttpStatus.SC_OK, "in progress", "");
        simulateUpdatingManifestGeneration(gitSecretsProcessor, anAcceptedPlanUpdateServiceInstanceRequest());
        polls_last_operation(operation, HttpStatus.SC_OK, "succeeded", "");

        assertThatThrownBy(() -> update_service_plan(aRejectedPlanUpdateServiceInstanceRequest()))
            .isInstanceOf(FeignException.class)
            .hasMessageContaining(("422"))
            .hasMessageContaining(("Service instance update not supported"))
            .hasMessageContaining(("List of supported plan upgrades from"));

        operation = upgrade_service();
        polls_last_operation(operation, HttpStatus.SC_OK, "in progress", "");
        simulateUpdatingManifestGeneration(gitSecretsProcessor, anUpgradeServiceInstanceRequest());
        polls_last_operation(operation, HttpStatus.SC_OK, "succeeded", "");


        delete_service_binding();

        turn_on_readonly_service_instances_mode();
        assert_create_service_instance_readonly_rejected();
        assert_delete_service_instance_readonly_rejected();
        create_service_binding();
        delete_service_binding();
        turn_off_readonly_service_instances_mode();

        String deleteOperation = delete_a_service_instance_using_osb_client();

        polls_last_operation(deleteOperation,
                HttpStatus.SC_GONE, //async delete expects a 410 status
                "succeeded", "succeeded");

        assertGitClonesWerePooledAndReturned();

    }

    private void turn_on_readonly_service_instances_mode() {
    	logger.info("turn_on_readonly_service_instances_mode: next service instances requests will be rejected");
        readOnlyServiceInstanceBrokerProcessor.setServiceInstanceReadOnlyMode(true);
    }

    private void turn_off_readonly_service_instances_mode() {
		logger.info("turn_off_readonly_service_instances_mode: next service instances requests will be accepted");
        readOnlyServiceInstanceBrokerProcessor.setServiceInstanceReadOnlyMode(false);
    }

    private void assertGitClonesWerePooledAndReturned() {
        assertGitClonesWerePooledAndReturned(secretsGitProperties, secretsGitManager);
        assertGitClonesWerePooledAndReturned(templatesGitProperties, templatesGitManager);
    }

    private void assertGitClonesWerePooledAndReturned(GitProperties gitProperties, GitManager templatesGitManager) {
        if (gitProperties.isUsePooling()) {
            assertThat(templatesGitManager instanceof PooledGitManager).isTrue();
            PooledGitManager pooledGitManager = (PooledGitManager) templatesGitManager;
            long borrowed = pooledGitManager.getPoolAttribute(PooledGitManager.Metric.Borrowed);
            long returned = pooledGitManager.getPoolAttribute(PooledGitManager.Metric.Returned);
            long created = pooledGitManager.getPoolAttribute(PooledGitManager.Metric.Created);
            long destroyed = pooledGitManager.getPoolAttribute(PooledGitManager.Metric.Destroyed);
            assertThat(borrowed).isGreaterThanOrEqualTo(1L);
            assertThat(returned).isEqualTo(borrowed);
            assertThat(created).isBetween(1L,2L); //1 when only eager prefetching at start up triggers
            // 2 when evictor thread runs exactly during OSB request process (we have min_idle=1)
            assertThat(destroyed).isEqualTo(0); //we should not reach the maxIdlePerKey=8 repos.
            // See DEFAULT_MAX_IDLE_PER_KEY = 8 in GenericKeyedObjectPoolConfig
        }
    }

    private void create_service_binding() {

// Consider alternatives to wire mock recordings:
//        wireMockRule.stubFor(get(urlEqualTo("/v2/service_instances/" + SERVICE_INSTANCE_ID + "/service_bindings/" + SERVICE_BINDING_INSTANCE_ID))
//                .willReturn(aResponse()
//                .withBody("")
//                .withStatus(200)));

        CreateServiceInstanceBindingRequest serviceInstanceBindingRequest = aBindingRequest(SERVICE_INSTANCE_ID);
        serviceInstanceBindingRequest.setBindingId(SERVICE_BINDING_INSTANCE_ID);

        ResponseEntity<CreateServiceInstanceAppBindingResponse> bindResponse = serviceInstanceBindingService.createServiceInstanceBinding(
                SERVICE_INSTANCE_ID,
                SERVICE_BINDING_INSTANCE_ID,
                false,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(serviceInstanceBindingRequest.getOriginatingIdentity()),
                OsbConstants.X_Broker_API_Version_Value,
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
                false,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
                OsbConstants.X_Broker_API_Version_Value);
    }

    /**
     * Request an async provisionning
     * @return the last operation field
     */
    private String create_async_service_instance_using_osb_client() {

        CreateServiceInstanceRequest createServiceInstanceRequest = aCreateServiceInstanceRequest();
                createServiceInstanceRequest.setServiceInstanceId(SERVICE_INSTANCE_ID);
        ResponseEntity<CreateServiceInstanceResponse> createResponse = serviceInstanceService.createServiceInstance(
                SERVICE_INSTANCE_ID,
                true,
                "api-info",
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
                OsbConstants.X_Broker_API_Version_Value,
                createServiceInstanceRequest);
        assertThat(createResponse.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getDashboardUrl())
            .as("dashboard url template configured in application.properties")
            .isEqualTo("https://grafana_" + BROKERED_SERVICE_INSTANCE_ID + ".redacted-ops-domain.com");
        return createResponse.getBody().getOperation();
    }

    // Equivalent of "cf update-service -p"
    private String update_service_plan(UpdateServiceInstanceRequest updateServiceInstanceRequest) {
        return update_service_instance(updateServiceInstanceRequest);
    }

    /**
     * Request an async update (equivalent of `cf service --upgrade`)
     * @return the last operation field
     */
    private String upgrade_service() {
        return update_service_instance(anUpgradeServiceInstanceRequest());
    }

    /**
     * Request an async update
     */
    private String update_service_instance(UpdateServiceInstanceRequest updateServiceInstanceRequest) {
        ResponseEntity<UpdateServiceInstanceResponse> updateResponse = serviceInstanceService.updateServiceInstance(
            SERVICE_INSTANCE_ID,
            true,
            "api-info",
            osbProxy.buildOriginatingIdentityHeader(updateServiceInstanceRequest.getOriginatingIdentity()),
            OsbConstants.X_Broker_API_Version_Value,
            updateServiceInstanceRequest);
        assertThat(updateResponse.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getDashboardUrl())
            .as("dashboard url template configured in application.properties")
            .isEqualTo("https://grafana_" + BROKERED_SERVICE_INSTANCE_ID + ".redacted-ops-domain.com");
        return updateResponse.getBody().getOperation();
    }

    // Equivalent of "cf service-update -p plan"
    @NotNull
    private UpdateServiceInstanceRequest anAcceptedPlanUpdateServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId(SERVICE_DEFINITION_ID)
            .planId(MEDIUM_SERVICE_PLAN_ID)
            .plan(OsbBuilderHelper.aMediumPlan())
            .serviceInstanceId(SERVICE_INSTANCE_ID)
            .parameters(OsbBuilderHelper.osbCmdbCustomParam(BROKERED_SERVICE_INSTANCE_ID))
            .context(aCfUserContext())
            .originatingIdentity(aCfUserContext())
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                aCreateServiceInstanceRequest().getPlanId(),
                null))
            .build();
    }

    // Equivalent of "cf service-update -p plan"
    @NotNull
    private UpdateServiceInstanceRequest aRejectedPlanUpdateServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId(SERVICE_DEFINITION_ID)
            .planId(MEDIUM_SERVICE_PLAN_ID)
            .plan(OsbBuilderHelper.aMediumPlan())
            .serviceInstanceId(SERVICE_INSTANCE_ID)
            .parameters(OsbBuilderHelper.osbCmdbCustomParam(BROKERED_SERVICE_INSTANCE_ID))
            .context(aCfUserContext())
            .originatingIdentity(aCfUserContext())
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                OsbBuilderHelper.aLargePlan().getId(),
                null))
            .build();
    }

    // Equivalent of "cf service-update --upgrade"
    @NotNull
    private UpdateServiceInstanceRequest anUpgradeServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId(SERVICE_DEFINITION_ID)
            .planId(UPGRADED_SERVICE_PLAN_ID)
            .serviceInstanceId(SERVICE_INSTANCE_ID)
            .maintenanceInfo(MaintenanceInfo.builder()
                .version(2,0,0, "")
                .description("Includes dashboards")
                .build())
            .parameters(OsbBuilderHelper.osbCmdbCustomParam(BROKERED_SERVICE_INSTANCE_ID))
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                UPGRADED_SERVICE_PLAN_ID,
                aCreateServiceInstanceRequest().getMaintenanceInfo()))
            .context(aCfUserContext())
            .originatingIdentity(aCfUserContext())
            .build();
    }

    private void assert_create_service_instance_readonly_rejected() {

        CreateServiceInstanceRequest createServiceInstanceRequest = aCreateServiceInstanceRequest();
        String serviceInstanceId = "aNewIdForReadOnlyRequestShouldBeRejected";
        createServiceInstanceRequest.setServiceInstanceId(serviceInstanceId);
        //noinspection CodeBlock2Expr
        assertThatThrownBy(
			() -> {
				serviceInstanceService.createServiceInstance(
					serviceInstanceId,
					true,
					"api-info",
					osbProxy.buildOriginatingIdentityHeader(createServiceInstanceRequest.getOriginatingIdentity()),
					OsbConstants.X_Broker_API_Version_Value,
					createServiceInstanceRequest);
			})
			.isInstanceOf(FeignException.ServiceUnavailable.class)
			.hasMessageContaining(DeploymentProperties.DEFAULT_READ_ONLY_MESSAGE);
	}

    public void exposes_catalog() {
        Catalog catalog = catalogServiceClient.getCatalog(OsbConstants.X_Broker_API_Version_Value);
        assertThat(catalog.getServiceDefinitions()).isNotEmpty();
        assertThat(catalog).isNotNull();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertThat(serviceDefinition).isNotNull();
        Plan defaultPlan = serviceDefinition.getPlans().get(0);
        assertThat(defaultPlan).isNotNull();
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
                osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
                OsbConstants.X_Broker_API_Version_Value);
        assertThat(response.getStatusCode()).isEqualTo(ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getOperation();
    }

    private void assert_delete_service_instance_readonly_rejected() {

        //noinspection CodeBlock2Expr
        assertThatThrownBy(
			() -> {
				serviceInstanceService.deleteServiceInstance(
					SERVICE_INSTANCE_ID,
					SERVICE_DEFINITION_ID,
					SERVICE_PLAN_ID,
					true,
					"api-info",
					osbProxy.buildOriginatingIdentityHeader(aCfUserContext()),
					OsbConstants.X_Broker_API_Version_Value);
			})
			.isInstanceOf(FeignException.ServiceUnavailable.class)
			.hasMessageContaining(DeploymentProperties.DEFAULT_READ_ONLY_MESSAGE);
    }


    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {
        return CreateServiceInstanceRequest.builder()
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .planId(SERVICE_PLAN_ID)
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .parameters(OsbBuilderHelper.osbCmdbCustomParam(BROKERED_SERVICE_INSTANCE_ID))
                .maintenanceInfo(MaintenanceInfo.builder()
                    .version(1,0,0, "")
                    .description("Initial version")
                    .build())
                .context(aCfUserContext())
                .originatingIdentity(aCfUserContext())
                .build();
    }

}
