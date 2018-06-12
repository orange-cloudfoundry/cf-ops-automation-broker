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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;

// $ tree coab-depls/
//coab-depls/
//├── cassandravarsops
//│   ├── deployment-dependencies.yml
//│   ├── operators
//│   │   └── coab-operators.yml
//│   └── template
//│       ├── cassandravarsops-vars.yml
//│       ├── cassandravarsops.yml
//│       ├── coab-operators.yml
//│       └── coab-vars.yml
//├── c_1cc4bd10-aadc-4d7d-a1c4-acb955e637db
//│   ├── deployment-dependencies.yml
//│   └── template
//│       ├── c_1cc4bd10-aadc-4d7d-a1c4-acb955e637db-vars.yml -> ../../cassandravarsops/template/cassandravarsops-vars.yml
//│       ├── c_1cc4bd10-aadc-4d7d-a1c4-acb955e637db.yml -> ../../cassandravarsops/template/cassandravarsops.yml
//│       ├── coab-operators.yml -> ../../cassandravarsops/operators/coab-operators.yml
//│       └── coab-vars.yml
//13 directories, 28 files
public class TemplatesGeneratorTest {

    private static final String REPOSITORY_DIRECTORY = "paas-templates";
    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
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
                this.deploymentProperties.getOperators(), "c"
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
    }//superclass TU

    @Test
    public void raise_exception_if_model_deployment_directory_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);

        //Given (a part is initialized by setUp method)
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment())
                .build();

        //When
        this.templatesGenerator.checkPrerequisites(this.workDir);
    }//superclass TU

    @Test
    public void raise_exception_if_template_directory_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.TEMPLATE_EXCEPTION);

        //When
        this.templatesGenerator.checkThatTemplateDirectoryExists(this.workDir);
    }

    @Test
    public void raise_exception_if_operators_directory_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.OPERATORS_EXCEPTION);

        //When
        this.templatesGenerator.checkThatOperatorsDirectoryExists(this.workDir);
    }

    @Test
    public void raise_exception_if_model_manifest_file_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.MANIFEST_FILE_EXCEPTION);

        //When
        this.templatesGenerator.checkThatModelManifestFileExists(this.workDir);
    }

    @Test
    public void raise_exception_if_model_vars_file_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.VARS_FILE_EXCEPTION);

        //When
        this.templatesGenerator.checkThatModelVarsFileExists(this.workDir);
    }

    @Test
    public void raise_exception_if_coab_operators_file_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);

        //When
        this.templatesGenerator.checkThatModelCoabOperatorsFileExists(this.workDir);
    }

    @Test
    public void check_that_all_prerequisites_are_satisfied() {

        //Given a model structure that meets all prerequisites
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate())
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION) //mongodb.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION) //coab-vars.yml
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators())
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators()},
                        DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION) //coab-operators.yml
                .build();

        //When
        this.templatesGenerator.checkPrerequisites(this.workDir);
    }

    @Test
    public void check_that_deployment_directory_is_generated() throws IOException {
        //Given
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment());
        Files.createDirectories(rootDeploymentDir);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path serviceInstanceDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID)
        );
        assertThat("Deployment directory doesn't exist", Files.exists(serviceInstanceDir));
    }

    @Test
    public void check_that_template_directory_is_generated() throws IOException {
        //Given
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment());
        Files.createDirectories(rootDeploymentDir);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate()
        );
        assertThat("Template directory doesn't exist", Files.exists(templateDir));
    }

    @Test
    public void check_that_deployment_dependencies_file_is_generated() throws IOException {
        //Given
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment());
        Files.createDirectories(rootDeploymentDir);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path deploymentDependenciesFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME
        );
        assertThat("Deployment dependencies file doesn't exist:" + deploymentDependenciesFile, Files.exists(deploymentDependenciesFile));
    }

    @Test
    public void check_that_symlink_towards_manifest_file_is_generated() throws IOException {
        //Given repository, root deployment,model deployment and template directory with model manifest file
        Path templateDir = aDeploymentTemplateDir();
        Path sourceManifestFile = StructureGeneratorHelper.generatePath(templateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION);
        Files.createFile(sourceManifestFile);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getModelDeploymentShortAlias() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards manifest file doesn't exist:" + targetManifestFile, Files.exists(targetManifestFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(targetManifestFile));
    }

    private Path aDeploymentTemplateDir() throws IOException {
        Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate());
        Files.createDirectories(templateDir);
        return templateDir;
    }

    @Test
    public void check_that_symlink_towards_vars_file_is_generated() throws IOException {
        //Given repository, root deployment,model deployment and template directory with model vars file
        Path templateDir = getTemplateDir();
        Files.createDirectories(templateDir);
        Path sourceVarsFile = StructureGeneratorHelper.generatePath(templateDir, this.deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
        Files.createFile(sourceVarsFile);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getModelDeploymentShortAlias() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(targetVarsFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(targetVarsFile));
//            assertThat(Files.readSymbolicLink(targetOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    private Path getTemplateDir() {
        return StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate());
    }

    @Test
    public void check_that_symlink_towards_coab_operators_file_is_generated() throws Exception {
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
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards coab operators file doesn't exist", Files.exists(targetOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(targetOperatorsFile));
        assertThat(Files.readSymbolicLink(targetOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    @Test
    public void check_that_coab_vars_file_is_generated() throws IOException {
        //Given
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment());
        Files.createDirectories(rootDeploymentDir);

        //When
        this.templatesGenerator.generate(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path coabVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION
        );
        assertThat("Coab vars file doesn't exist", Files.exists(coabVarsFile));
    }

    @Test
    public void check_that_symlinks_towards_operators_files_under_templates_directory_are_generated_bis() throws Exception {

        //Given model and deployment structure
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(),
                                this.deploymentProperties.getModelDeployment(),
                                this.deploymentProperties.getTemplate()},
                        "1-add-shield-operators.yml").build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),
                                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                                this.deploymentProperties.getTemplate()).build();

        //When
        this.templatesGenerator.generateOperatorsFileSymLinks(workDir, SERVICE_INSTANCE_ID);

        //Then
        Path expectedFirstOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "1-add-shield-operators.yml");
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedFirstOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(expectedFirstOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedFirstOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/template/1-add-shield-operators.yml")));

    }

    @Test
    public void check_that_symlinks_towards_operators_files_under_templates_directory_are_generated() throws Exception {
        //Given repository, root deployment, model deployment and several operators files under templates directory
        Path modelTemplatesDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate());
        Files.createDirectories(modelTemplatesDir);
        Path firstOperatorFile = StructureGeneratorHelper.generatePath(modelTemplatesDir, "1-add-shield-operators.yml");
        Files.createFile(firstOperatorFile);
        //Given deployment directory
        Path deploymentTemplateDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate());
        Files.createDirectories(deploymentTemplateDir);

        //When
        this.templatesGenerator.generateOperatorsFileSymLinks(workDir, SERVICE_INSTANCE_ID);

        //Then
        Path expectedFirstOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentInstance(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "1-add-shield-operators.yml");
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedFirstOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(expectedFirstOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedFirstOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/template/1-add-shield-operators.yml")));

    }





    private DeploymentProperties aDeploymentProperties() {
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("cassandravarsops");
        deploymentProperties.setTemplate("template");
        deploymentProperties.setVars("vars");
        deploymentProperties.setOperators("operators");
        return deploymentProperties;
    }

    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void populatePaasTemplates() {
        Path workDir = Paths.get("/home/ijly7474/GIT/coab/paas-templates");
        this.templatesGenerator.checkPrerequisites(workDir);
        this.templatesGenerator.generate(workDir, SERVICE_INSTANCE_ID);
    }
}