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
    public void raise_exception_if_root_deployment_directory_is_missing() {
        try {
            //Then
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.ROOT_DEPLOYMENT_EXCEPTION);
            //Given repository directory
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            //When
            TemplatesGenerator templates = new TemplatesGenerator(file.toPath(), "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_deployment_directory_is_missing() {
        try {
            //Then
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.MODEL_DEPLOYMENT_EXCEPTION);
            //Given repository and root deployment directory
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(file.toPath(), CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
            rootDeploymentDir = Files.createDirectory(rootDeploymentDir);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(file.toPath(), "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_template_directory_is_missing() {
        try {
            //Then
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.TEMPLATE_EXCEPTION);
            //Given repository, root deployment and model deployment directory
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(file.toPath(),
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
            modelDeploymentDir = Files.createDirectories(modelDeploymentDir);
            //When
            TemplatesGenerator templates = new TemplatesGenerator(file.toPath(), "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_vars_file_is_missing() {
        try {
            //Then
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.VARS_FILE_EXCEPTION);
            //Given repository, root deployment, model, template deployment directory and manifest file
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(file.toPath(),
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);
            //When
            TemplatesGenerator templates = new TemplatesGenerator(file.toPath(), "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_manifest_file_is_missing() {
        try {
            //Then
            thrown.expect(CassandraProcessorException.class);
            thrown.expectMessage(CassandraProcessorConstants.MANIFEST_FILE_EXCEPTION);
            //Given repository, root deployment, model, template deployment directory and vars file
            File file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(file.toPath(),
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(file.toPath(), "");
            templates.checkPrerequisites();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_deployment_directory_is_generated() {
        try {

            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID
            );
            assertThat("Deployment directory doesn't exist", Files.exists(serviceInstanceDir));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_template_directory_is_generated() {
        try {

            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY
            );
            assertThat("Template directory doesn't exist", Files.exists(templateDir));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_deployment_dependencies_file_is_generated() {
        try {

            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path deploymentDependenciesFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME
            );
            assertThat("Deployment dependencies file doesn't exist", Files.exists(deploymentDependenciesFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_operators_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path operatorsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.OPERATORS_FILENAME
            );
            assertThat("Operators file doesn't exist", Files.exists(operatorsFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_if_symlink_towards_manifest_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path manifestFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.MANIFEST_FILENAME_SUFFIX);
            assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(manifestFile));
            assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(manifestFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_if_symlink_towards_vars_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and template directory
            Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            modelTemplateDir = Files.createDirectories(modelTemplateDir);
            //Given model vars file and manifest file
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_VARS_FILENAME);
            modelVarsFile = Files.createFile(modelVarsFile);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            modelManifestFile = Files.createFile(modelManifestFile);

            //When
            TemplatesGenerator templates = new TemplatesGenerator(workDir, SERVICE_INSTANCE_ID);
            templates.checkPrerequisites();
            templates.generate();

            //Then
            Path varsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.VARS_FILENAME_SUFFIX);
            assertThat("Symbolic link towards vars file doesn't exist", Files.exists(varsFile));
            assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(varsFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void testPathAdvanced() throws IOException {
        //Given repository, root deployment,model deployment and template directory
        Path workDir = Files.createTempDirectory(REPOSITORY_DIRECTORY);

        Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY);
        modelTemplateDir = Files.createDirectories(modelTemplateDir);

        Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
        modelManifestFile = Files.createFile(modelManifestFile);

        Path serviceTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY);
        serviceTemplateDir = Files.createDirectories(serviceTemplateDir);

        Path serviceToModel = serviceTemplateDir.relativize(modelTemplateDir);
        System.out.println(serviceToModel);
        Path relativeModelManifestFile = StructureGeneratorHelper.generatePath(serviceToModel,
                CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
        System.out.println(relativeModelManifestFile);
        Path serviceManifestFile = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.MANIFEST_FILENAME_SUFFIX);
        Files.createSymbolicLink(serviceManifestFile, relativeModelManifestFile);

    }

    @Test@Ignore
    public void populatePaasTemplates() throws IOException {
        Path workDir = Paths.get("/home/ijly7474/GIT/paas-templates");
        String serviceInstanceId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
        TemplatesGenerator templates = new TemplatesGenerator(workDir, serviceInstanceId);
        templates.checkPrerequisites();
        templates.generate();
    }


}