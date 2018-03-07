package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.MatcherAssert.assertThat;

public class SecretsGeneratorTest {

    public static final String REPOSITORY_DIRECTORY = "paas-secrets";
    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
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
    public void raise_exception_if_model_deployment_directory_is_missing(){
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
    public void raise_exception_if_secrets_directory_is_missing(){
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.SECRETS_EXCEPTION);
            //Given (a part is initialized by setUp method)
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);
            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_meta_file_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.META_FILE_EXCEPTION);
            //Given (a part is initialized by setUp method) : root deployment, model and secrets directory
            Path modelSecretsDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getSecrets());
            Files.createDirectories(modelSecretsDir);
            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_secrets_file_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.SECRETS_FILE_EXCEPTION);
            //Given (a part is initialized by setUp method) : root deployment, model, secrets directory and meta file
            Path modelSecretsDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), deploymentProperties.getSecrets());
            Files.createDirectories(modelSecretsDir);
            Path modelMetaFile = StructureGeneratorHelper.generatePath(modelSecretsDir, this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(modelMetaFile);
            //When
            this.secretsGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Test
    public void check_that_deployment_instance_directory_is_generated() {
        try {
            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);

            //When
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
    public void check_that_secrets_directory_is_generated() {
        try {
            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);

            //When
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
    public void check_that_meta_file_is_generated() {
        try {

            //Given
            Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getSecrets());
            secretsDir = Files.createDirectories(secretsDir);
            Path sourceMetaFile = StructureGeneratorHelper.generatePath(secretsDir, this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(sourceMetaFile);

            //When
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
    public void check_that_secrets_file_is_generated() {
        try {

            //Given
            Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getSecrets());
            secretsDir = Files.createDirectories(secretsDir);
            Path sourceSecretsFile = StructureGeneratorHelper.generatePath(secretsDir, this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(sourceSecretsFile);

            //When
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
    public void check_that_enable_deployment_file_is_generated() {
        try {

            //Given
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);

            //When
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
    public void check_that_enable_deployment_file_is_removed() {

        //Given
        this.check_that_enable_deployment_file_is_generated();

        //When
        this.secretsGenerator.remove(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetEnableDeploymentFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME);
        assertThat("Enable deployment file is still existing", Files.notExists(targetEnableDeploymentFile));

    }


    @Test
    public void check_that_meta_file_is_removed() {

        //Given
        this.check_that_meta_file_is_generated();

        //When
        this.secretsGenerator.remove(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetMetaFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
        assertThat("Meta file is still existing", Files.notExists(targetMetaFile));
    }



    @Test
    public void check_that_secrets_file_is_removed() {
        //Given
        this.check_that_secrets_file_is_generated();

        //When
        this.secretsGenerator.remove(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetSecretsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
        assertThat("Secrets file is still existing", Files.notExists(targetSecretsFile));
    }

    private DeploymentProperties aDeploymentProperties(){
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("cassandravarsops");
        deploymentProperties.setSecrets("secrets");
        deploymentProperties.setMeta("meta");
        return deploymentProperties;
    }

    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void populatePaasSecrets() throws IOException {
        Path workDir = Paths.get("/home/ijly7474/GIT/coab/preprod-secrets");
        this.secretsGenerator.checkPrerequisites(workDir);
        this.secretsGenerator.generate(workDir, SERVICE_INSTANCE_ID);
    }

}
