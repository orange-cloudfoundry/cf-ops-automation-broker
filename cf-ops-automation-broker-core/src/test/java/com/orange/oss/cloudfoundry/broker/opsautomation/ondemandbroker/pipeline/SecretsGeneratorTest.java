package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;

public class SecretsGeneratorTest {

    public static final String REPOSITORY_DIRECTORY = "paas-secrets";
    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private DeploymentProperties deploymentProperties;
    private File file;
    private Path workDir;

    private SecretsGenerator secretsGenerator;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        this.deploymentProperties = aDeploymentProperties();
        this.file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
        this.workDir = file.toPath();
        this.secretsGenerator = new SecretsGenerator(this.deploymentProperties.getRootDeployment(),
                                    this.deploymentProperties.getModelDeployment(),
                                    this.deploymentProperties.getSecrets(),
                                    this.deploymentProperties.getMeta()
                );
    }

    @Test
    public void raise_exception_if_root_deployment_is_missing(){
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);
            //Given initialized by setUp method
            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
    }

    @Test
    public void raise_exception_if_model_deployment_is_missing(){
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);
            //Given (a part is initialized by setUp method)
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment());
            Files.createDirectory(rootDeploymentDir);
            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_deployment_instance_directory_is_generated() {
        try {
            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);

            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
            this.secretsGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path deploymentInstanceDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID
            );
            assertThat("Deployment directory doesn't exist", Files.exists(deploymentInstanceDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_secrets_directory_is_generated() {
        try {
            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            modelDeploymentDir = Files.createDirectories(modelDeploymentDir);

            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
            this.secretsGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getSecrets()
            );
            assertThat("Secrets directory doesn't exist", Files.exists(secretsDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_meta_file_is_generated() {
        try {

            //Given
            Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getSecrets());
            secretsDir = Files.createDirectories(secretsDir);
            Path sourceMetaFile = StructureGeneratorHelper.generatePath(secretsDir, this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
            sourceMetaFile = Files.createFile(sourceMetaFile);

            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
            this.secretsGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetMetaFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getSecrets(),
                    this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
            assertThat("Meta file doesn't exist", Files.exists(targetMetaFile));
            assertThat("Meta file is not a symbolic link", Files.isSymbolicLink(targetMetaFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_secrets_file_is_generated() {
        try {

            //Given
            Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getSecrets());
            secretsDir = Files.createDirectories(secretsDir);
            Path sourceSecretsFile = StructureGeneratorHelper.generatePath(secretsDir, this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
            sourceSecretsFile = Files.createFile(sourceSecretsFile);

            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
            this.secretsGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetSecretsFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getSecrets(),
                    this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
            assertThat("Secrets file doesn't exist", Files.exists(targetSecretsFile));
            assertThat("Secrets file is not a symbolic link", Files.isSymbolicLink(targetSecretsFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_enable_deployment_file_is_generated() {
        try {

            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            modelDeploymentDir = Files.createDirectories(modelDeploymentDir);

            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
            this.secretsGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetEnableDeploymentFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME);
            assertThat("Enable deployment file doesn't exist", Files.exists(targetEnableDeploymentFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_enable_deployment_file_is_removed() {

        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
            serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
            Path enableDeploymentFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.ENABLE_DEPLOYMENT_FILENAME);
            Files.write(enableDeploymentFile, Arrays.asList(CassandraProcessorConstants.ENABLE_DEPLOYMENT_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //When
            SecretsGenerator secrets = new SecretsGenerator();
            secrets.remove(workDir, SERVICE_INSTANCE_ID);

            //Then
            assertThat("Enable deployment file exists", Files.notExists(enableDeploymentFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_meta_file_is_removed() {

        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path secretsDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY);
            secretsDir = Files.createDirectories(secretsDir);

            Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.META_FILENAME);
            Files.write(metaFile, Arrays.asList(CassandraProcessorConstants.META_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //When
            SecretsGenerator secrets = new SecretsGenerator();
            secrets.remove(workDir, SERVICE_INSTANCE_ID);

            //Then
            assertThat("Meta file exists", Files.notExists(metaFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_secrets_file_is_removed() {

        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path secretsDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY);
            secretsDir = Files.createDirectories(secretsDir);

            Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.SECRETS_FILENAME);
            Files.write(secretsFile, Arrays.asList(CassandraProcessorConstants.SECRETS_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //When
            SecretsGenerator secrets = new SecretsGenerator();
            secrets.remove(workDir, SERVICE_INSTANCE_ID);

            //Then
            assertThat("Secrets file exists", Files.notExists(secretsFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    @Test
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test@Ignore
    public void populatePaasTemplates() throws IOException {
        Path workDir = Paths.get("/home/ijly7474/GIT/bosh-cloudwatt-secrets-pprod");
        String serviceInstanceId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
        SecretsGenerator secrets = new SecretsGenerator();
        secrets.checkPrerequisites(workDir);
        secrets.generate(workDir, serviceInstanceId);
    }

    private DeploymentProperties aDeploymentProperties(){
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("deployment");
        deploymentProperties.setSecrets("secrets");
        deploymentProperties.setMeta("meta");
        return deploymentProperties;
    }

}
