package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
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

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    public static final String REPOSITORY_DIRECTORY = "paas-secrets";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void raise_exception_if_root_deployment_is_missing(){
        try {
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.ROOT_DEPLOYMENT_EXCEPTION);
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path workDir = file.toPath();
            SecretsGenerator secrets = new SecretsGenerator(workDir, "");
            secrets.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_deployment_is_missing(){
        try {
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.MODEL_DEPLOYMENT_EXCEPTION);
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path workDir = file.toPath();
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(file.toPath(), CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            SecretsGenerator secrets = new SecretsGenerator(workDir, "");
            secrets.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_folders_are_generated() {
        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(workDir, CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(rootDeploymentDir, CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
            modelDeploymentDir = Files.createDirectory(modelDeploymentDir);

            //When
            SecretsGenerator secrets = new SecretsGenerator(workDir, SERVICE_INSTANCE_ID);
            secrets.checkPrerequisites();
            secrets.generate();

            //Then
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID
            );
            Path secretsDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY
            );
            assertThat("Deployment directory doesn't exist", Files.exists(serviceInstanceDir));
            assertThat("Secrets directory doesn't exist", Files.exists(secretsDir));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_files_are_generated() {
        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(workDir, CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(rootDeploymentDir, CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
            modelDeploymentDir = Files.createDirectory(modelDeploymentDir);

            //When
            SecretsGenerator secrets = new SecretsGenerator(workDir, SERVICE_INSTANCE_ID);
            secrets.checkPrerequisites();
            secrets.generate();

            //Then
            Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.META_FILENAME
            );
            Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.SECRETS_FILENAME
            );
            Path enableDeploymentFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.ENABLE_DEPLOYMENT_FILENAME
            );

            assertThat("Meta file doesn't exist", Files.exists(metaFile));
            assertThat("Secrets file doesn't exist", Files.exists(secretsFile));
            assertThat("Enable deployment file doesn't exist", Files.exists(enableDeploymentFile));

            
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_files_content_are_correct() {
        //TODO
    }


}