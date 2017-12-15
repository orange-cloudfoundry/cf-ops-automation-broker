package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class TemplatesGeneratorTest {

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    public static final String REPOSITORY_DIRECTORY = "paas-templates";

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
            TemplatesGenerator templates = new TemplatesGenerator(workDir, "");
            templates.checkPrerequisites();
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
            Path rootDeploymentDir = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            TemplatesGenerator templates = new TemplatesGenerator(workDir, "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_folders_are_generated() {
        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path rootDeploymentDir = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            Path modelDeploymentDir = Paths.get(String.valueOf(rootDeploymentDir) + File.separator + CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
            modelDeploymentDir = Files.createDirectory(modelDeploymentDir);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path serviceInstanceDir = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
            Path templateDir = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + File.separator + CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            assertThat("Deployment directory doesn't exist", Files.exists(serviceInstanceDir));
            assertThat("Template directory doesn't exist", Files.exists(templateDir));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_files_are_generated() {
        try {

            //Given
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path rootDeploymentDir = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);
            Path modelDeploymentDir = Paths.get(String.valueOf(rootDeploymentDir) + File.separator + CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
            modelDeploymentDir = Files.createDirectory(modelDeploymentDir);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path deploymentDependenciesFile = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + File.separator + CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME);

            assertThat("Deployment dependencies file doesn't exist", Files.exists(deploymentDependenciesFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    @Test@Ignore
    public void test(){

        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("cassandra/deployment-dependencies.yml").toURI()));
            System.out.println(lines);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
