package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.context.TestExecutionListeners;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class TemplatesGeneratorTest {

    public static final String REPOSITORY_DIRECTORY = "paas-templates";
    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private DeploymentProperties deploymentProperties;
    private File file;
    private Path workDir;

    private TemplatesGenerator templatesGenerator;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        this.deploymentProperties = aDeploymentProperties();
        this.file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
        this.workDir = file.toPath();
        this.templatesGenerator = new TemplatesGenerator(this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getVars(),
                this.deploymentProperties.getOperators()
        );
    }

    @Test
    public void raise_exception_if_root_deployment_directory_is_missing() {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);
            //Given initialized by setUp method
            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
    }

    @Test
    public void raise_exception_if_model_deployment_directory_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);
            //Given (a part is initialized by setUp method)
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment());
            Files.createDirectory(rootDeploymentDir);
            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_template_directory_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.TEMPLATE_EXCEPTION);
            //Given (a part is initialized by setUp method)
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment());
            Files.createDirectories(modelDeploymentDir);
            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_operators_directory_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.OPERATORS_EXCEPTION);
            //Given (a part is initialized by setUp method)
            Path modelDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate());
            Files.createDirectories(modelDeploymentDir);
            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_manifest_file_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.MANIFEST_FILE_EXCEPTION);

            //Given (a part is initialized by setUp method) : root deployment, model, template directory and operators directory
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate());
            Files.createDirectories(modelTemplateDir);
            Path modelOperatorsDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators());
            Files.createDirectories(modelOperatorsDir);

            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_model_vars_file_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.VARS_FILE_EXCEPTION);

            //Given repository, root deployment, model, template deployment directory, operators directory and manifest file
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), deploymentProperties.getTemplate());
            Files.createDirectories(modelTemplateDir);
            Path modelOperatorsDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators());
            Files.createDirectories(modelOperatorsDir);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(modelManifestFile);

            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void raise_exception_if_coab_operators_file_is_missing() {
        try {
            //Then
            thrown.expect(DeploymentException.class);
            thrown.expectMessage(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);

            //Given repository, root deployment, model, template deployment directory, operators directory, manifest file and model vars file
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), deploymentProperties.getTemplate());
            Files.createDirectories(modelTemplateDir);
            Path modelOperatorsDir = StructureGeneratorHelper.generatePath(this.workDir, this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators());
            Files.createDirectories(modelOperatorsDir);
            Path modelManifestFile = StructureGeneratorHelper.generatePath(modelTemplateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(modelManifestFile);
            Path modelVarsFile = StructureGeneratorHelper.generatePath(modelTemplateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(modelVarsFile);

            //When
            this.templatesGenerator.checkPrerequisites(this.workDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_all_prerequisites_are_satisfied(){

    }

    @Test
    public void check_that_deployment_directory_is_generated() {
        try {

            //Given
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment());
            Files.createDirectories(rootDeploymentDir);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID
            );
            assertThat("Deployment directory doesn't exist", Files.exists(serviceInstanceDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_template_directory_is_generated() {
        try {
            //Given
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment());
            Files.createDirectories(rootDeploymentDir);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getTemplate()
            );
            assertThat("Template directory doesn't exist", Files.exists(templateDir));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_deployment_dependencies_file_is_generated() {
        try {

            //Given
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment());
            Files.createDirectories(rootDeploymentDir);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path deploymentDependenciesFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME
            );
            assertThat("Deployment dependencies file doesn't exist", Files.exists(deploymentDependenciesFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_symlink_towards_manifest_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and template directory with model manifest file
            Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getTemplate());
            Files.createDirectories(templateDir);
            Path sourceManifestFile = StructureGeneratorHelper.generatePath(templateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(sourceManifestFile);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getTemplate(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID + DeploymentConstants.YML_EXTENSION);
            assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(targetManifestFile));
            assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(targetManifestFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void check_that_symlink_towards_vars_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and template directory with model vars file
            Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getTemplate());
            Files.createDirectories(templateDir);
            Path sourceVarsFile = StructureGeneratorHelper.generatePath(templateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(sourceVarsFile);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getTemplate(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
            assertThat("Symbolic link towards vars file doesn't exist", Files.exists(targetVarsFile));
            assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(targetVarsFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_symlink_towards_coab_operators_file_is_generated() {
        try {
            //Given repository, root deployment,model deployment and operators directory with coab-operators file
            Path operatorsDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment(),
                    this.deploymentProperties.getOperators());
            Files.createDirectories(operatorsDir);
            Path sourceOperatorsFile = StructureGeneratorHelper.generatePath(operatorsDir, DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION);
            Files.createFile(sourceOperatorsFile);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path targetOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getTemplate(),
                    DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION);
            assertThat("Symbolic link towards coab operators file doesn't exist", Files.exists(targetOperatorsFile));
            assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(targetOperatorsFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void check_that_coab_vars_file_is_generated() {
        try {

            //Given
            Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment());
            Files.createDirectories(rootDeploymentDir);

            //When
            this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

            //Then
            Path coabVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                    this.deploymentProperties.getRootDeployment(),
                    this.deploymentProperties.getModelDeployment() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                    this.deploymentProperties.getTemplate(),
                    DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION
            );
            assertThat("Coab vars file doesn't exist", Files.exists(coabVarsFile));

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

    private DeploymentProperties aDeploymentProperties(){
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("deployment");
        deploymentProperties.setTemplate("template");
        deploymentProperties.setVars("vars");
        deploymentProperties.setOperators("operators");
        return deploymentProperties;
    }



    @Test@Ignore
    public void populatePaasTemplates() throws IOException {
        Path workDir = Paths.get("/home/ijly7474/GIT/paas-templates");
        String serviceInstanceId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
        TemplatesGenerator templates = new TemplatesGenerator();
        templates.checkPrerequisites(workDir);
        templates.generate(workDir, SERVICE_INSTANCE_ID);
    }


}