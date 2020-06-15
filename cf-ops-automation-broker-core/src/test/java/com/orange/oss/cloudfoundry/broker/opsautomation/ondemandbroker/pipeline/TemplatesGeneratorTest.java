package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Copy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Tree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplatesGeneratorTest extends StructureGeneratorImplTest{

    private static String SYM_LINK = "../../";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        //Shared template generator
        this.templatesGenerator = new TemplatesGenerator(this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                "c",
                "_",
                new VarsFilesYmlFormatter());
        //Init sample deployments
        this.initReferenceModelStructures();
    }

    private TemplatesGenerator templatesGenerator;

    @Test
    public void raise_exception_if_template_directory_is_missing() {
        DeploymentException deploymentException = assertThrows(DeploymentException.class, () ->
        //When
                this.templatesGenerator.checkThatTemplateDirectoryExists(this.workDir));
        //then
        assertThat(deploymentException.getMessage(), startsWith(DeploymentConstants.TEMPLATE_EXCEPTION));
    }

    @Test
    public void raise_exception_if_operators_directory_is_missing() {
        DeploymentException deploymentException = assertThrows(DeploymentException.class, () ->
            //When
            this.templatesGenerator.checkThatOperatorsDirectoryExists(this.workDir));
        //then
        assertThat(deploymentException.getMessage(), startsWith(DeploymentConstants.OPERATORS_EXCEPTION));
    }

    @Test
    public void raise_exception_if_model_manifest_file_is_missing() {
        DeploymentException deploymentException = assertThrows(DeploymentException.class, () ->
            //When
            this.templatesGenerator.checkThatModelManifestFileExists(this.workDir));
        //then
        assertThat(deploymentException.getMessage(), startsWith(DeploymentConstants.MANIFEST_FILE_EXCEPTION));
    }

    @Test
    public void raise_exception_if_model_vars_file_is_missing() {
        DeploymentException deploymentException = assertThrows(DeploymentException.class, () ->
            //When
            this.templatesGenerator.checkThatModelVarsFileExists(this.workDir));
        //then
        assertThat(deploymentException.getMessage(), startsWith(DeploymentConstants.VARS_FILE_EXCEPTION));
    }

    @Test
    public void raise_exception_if_coab_operators_file_is_missing() {
        DeploymentException deploymentException = assertThrows(DeploymentException.class, () ->
            //When
            this.templatesGenerator.checkThatModelCoabOperatorsFileExists(this.workDir));
        //then
        assertThat(deploymentException.getMessage(), startsWith(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION));
    }

    @Test
    public void check_that_all_prerequisites_are_satisfied() {

        //Given : The model structure that meets all prerequisites is initialized in setup method

        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator("coab-depls",
                "areferencemodel",
                "r",
                "_",
                new VarsFilesYmlFormatter());

        //When
        templatesGenerator.checkPrerequisites(tempDir.toPath());
    }

    @Test
    public void check_that_template_directory_is_generated() {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID))
                .build();

        //When
        this.templatesGenerator.generateTemplateDirectory(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path templateDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE
        );
        assertThat("Template directory doesn't exist", Files.exists(templateDir));
    }

    @Test
    public void check_that_coab_vars_file_is_generated() throws IOException {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), DeploymentConstants.TEMPLATE)
                .build();
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //When
        this.templatesGenerator.generateCoabVarsFile(this.workDir, SERVICE_INSTANCE_ID, coabVarsFileDto);

        //Then
        Path coabVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + DeploymentConstants.VARS + DeploymentConstants.YML_EXTENSION
        );
        assertThat("Coab vars file should exist", Files.exists(coabVarsFile));
        assertThat("Coab vars file should contain deployment name", new String(Files.readAllBytes(coabVarsFile), StandardCharsets.UTF_8), containsString(coabVarsFileDto.deployment_name));
    }

    @Test
    public void check_that_symlink_towards_coab_operators_file_is_generated() throws Exception {
        //Given repository, root deployment,model deployment and operators directory with coab-operators file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.OPERATORS},
                        DeploymentConstants.COAB + DeploymentConstants.HYPHEN + DeploymentConstants.OPERATORS + DeploymentConstants.YML_EXTENSION)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), DeploymentConstants.TEMPLATE)
                .build();

        //When
        this.templatesGenerator.generateCoabOperatorsFileSymLink(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + DeploymentConstants.OPERATORS + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards coab operators file doesn't exist", Files.exists(targetOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(targetOperatorsFile));
        assertThat(Files.readSymbolicLink(targetOperatorsFile).toString(), is(equalTo(SYM_LINK + this.deploymentProperties.getModelDeployment() +
                        File.separator + DeploymentConstants.OPERATORS + File.separator +
                        DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX)));
    }

    @Test
    public void check_that_symlink_towards_deployment_dependencies_file_is_generated() throws Exception {
        //Given repository, root deployment,model deployment and operators directory with coab-operators file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment()},
                        DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID))
                .build();

        //When
        this.templatesGenerator.generateDeploymentDependenciesFileSymLink(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetDeploymentDependenciesFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME);
        assertThat("Symbolic link towards deployment dependencies file doesn't exist", Files.exists(targetDeploymentDependenciesFile));
        assertThat("Deployment dependencies file is not a symbolic link", Files.isSymbolicLink(targetDeploymentDependenciesFile));
        assertThat(Files.readSymbolicLink(targetDeploymentDependenciesFile).toString(), is(equalTo("../" + this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME)));
    }

    @Test
    public void check_that_all_symlinks_templates_directory_are_generated() throws Exception {

        //Given : The model structure is initialized in setup method
        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator("coab-depls",
                "areferencemodel",
                "r",
                "_",
                new VarsFilesYmlFormatter());

        //Given a minimal deployment structure
        Structure deploymentStructure = new Structure.StructureBuilder(tempDir.toPath())
                .withDirectoryHierarchy("coab-depls",
                        templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                        DeploymentConstants.TEMPLATE).build();

        //When
        templatesGenerator.generateAllSymLinks(tempDir.toPath(), SERVICE_INSTANCE_ID);

        //Then
        String expectedStructure = expectedStructure("coab-depls", "expected-areferencemodel-tree.txt", templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
        String generatedStructure = generatedStructure("coab-depls", "areferencemodel", templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
        assertEquals(expectedStructure, generatedStructure);
    }

    private void initReferenceModelStructures() throws IOException {
        //Given a template repository in /tmp
        Path paasTemplatePath = tempDir.toPath();

        //Search for the sample-deployment
        Path referenceDataModel = Paths.get("../sample-deployment");

        //Copy reference data model
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Copy.TreeCopier tc = new Copy.TreeCopier(referenceDataModel, paasTemplatePath, "coab-depls", false, true);
        Files.walkFileTree(referenceDataModel, opts, Integer.MAX_VALUE, tc);
    }

    @Test
    public void check_generation_against_sample_deployment_model() throws IOException {

        //Given : The model structure is initialized in setup method

        //Check all models
        checkDeployment("coab-depls", "mongodb", "m","_");
        checkDeployment("coab-depls", "cassandravarsops", "c", "_");
        checkDeployment("coab-depls", "cassandra", "s", "_");
        checkDeployment("coab-depls", "cf-mysql", "y", "_");

    }

    private void checkDeployment(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias, String modelDeploymentSeparator) throws IOException{

        //Given a path
        Path paasTemplatePath = tempDir.toPath();

        //Given and a user request
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator(rootDeployment,
                modelDeployment,
                modelDeploymentShortAlias,
                modelDeploymentSeparator,
                new VarsFilesYmlFormatter());

        //When
        templatesGenerator.checkPrerequisites(paasTemplatePath);
        templatesGenerator.generate(paasTemplatePath, SERVICE_INSTANCE_ID, coabVarsFileDto);

       //Then
       System.out.print("Checking : " + modelDeployment);
       String expectedStructure = expectedStructure(rootDeployment, "expected-" + modelDeployment + "-tree.txt", templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
       String generatedStructure = generatedStructure(rootDeployment, modelDeployment, templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
       //System.out.println(expectedStructure);
       //System.out.println(generatedStructure);
       assertEquals(expectedStructure, generatedStructure);
       System.out.println("=> Success");
    }

    private String expectedStructure(String rootDeployment, String expectedTreeFile, String deploymentName) throws IOException{
        Path referenceDataModel = Paths.get("../sample-deployment").resolve(rootDeployment).resolve(expectedTreeFile);
        List<String> expectedTree = Files.readAllLines(referenceDataModel);
        StringBuilder sb = new StringBuilder();
        for (String s:expectedTree){
            sb.append(s).append(System.getProperty("line.separator"));
        }
        return MessageFormat.format(sb.toString(), deploymentName);
    }

    private String generatedStructure(String rootDeployment, String modelDeployment, String deploymentName) {
        Path paasTemplatePath = tempDir.toPath();
        Path modelPath = StructureGeneratorHelper.generatePath(paasTemplatePath, rootDeployment, modelDeployment);
        Path deploymentPath = StructureGeneratorHelper.generatePath(paasTemplatePath, rootDeployment, deploymentName);
        return (new Tree().print(modelPath) + new Tree().print(deploymentPath));
    }

    protected CoabVarsFileDto aTypicalUserProvisionningRequest() {
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.instance_id = "service_instance_id";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";
        return coabVarsFileDto;
    }

    @Test
    @Disabled
    public void populateRealPaasTemplates() {

        //Given a path
        Path workDir = Paths.get("/home/losapio/GIT/Coab/paas-templates");

        //Given and a user request
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator("coab-depls",
                "cf-mysql",
                "y",
                "_",
                new VarsFilesYmlFormatter());

        //When
        templatesGenerator.checkPrerequisites(workDir);
        templatesGenerator.generate(workDir, SERVICE_INSTANCE_ID, coabVarsFileDto);

    }

}