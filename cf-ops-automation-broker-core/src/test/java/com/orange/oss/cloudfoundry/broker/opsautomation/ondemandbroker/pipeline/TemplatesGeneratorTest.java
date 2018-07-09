package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Copy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools.Tree;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

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
public class TemplatesGeneratorTest extends StructureGeneratorImplTest{

    private static String SYM_LINK = "../../";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Shared template generator
        this.templatesGenerator = new TemplatesGenerator(this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getOperators(),
                "c",
                new VarsFilesYmlFormatter());
    }

    private TemplatesGenerator templatesGenerator;

    @Test
    public void raise_exception_if_template_directory_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(startsWith(DeploymentConstants.TEMPLATE_EXCEPTION));

        //When
        this.templatesGenerator.checkThatTemplateDirectoryExists(this.workDir);
    }

    @Test
    public void raise_exception_if_operators_directory_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(startsWith(DeploymentConstants.OPERATORS_EXCEPTION));

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
        this.aModelStructure();

        //When
        this.templatesGenerator.checkPrerequisites(this.workDir);
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
    public void check_that_deployment_dependencies_file_is_generated() {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID))
                .build();

        //When
        this.templatesGenerator.generateDeploymentDependenciesFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path deploymentDependenciesFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME
        );
        assertThat("Deployment dependencies file doesn't exist:" + deploymentDependenciesFile, Files.exists(deploymentDependenciesFile));
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
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators()},
                        DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION)
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
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getOperators() + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards coab operators file doesn't exist", Files.exists(targetOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(targetOperatorsFile));
        assertThat(Files.readSymbolicLink(targetOperatorsFile).toString(), is(equalTo(SYM_LINK + this.deploymentProperties.getModelDeployment() +
                        File.separator + this.deploymentProperties.getOperators() + File.separator +
                        DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX)));
    }

    @Test
    public void check_that_all_symlinks_templates_directory_are_generated() throws Exception {

        //Given model and deployment structure
        this.aModelStructure();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),
                        this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                        DeploymentConstants.TEMPLATE).build();





        //When
        this.templatesGenerator.generateAllSymLinks(workDir, SERVICE_INSTANCE_ID);

        //Then
        this.assertManifestFile();
        this.assertVarsFile();
        this.assertOperatorsFile();
        this.assertIassTypeFile();
    }

    private void aModelStructure(){
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION) //model.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX) //model-tpl.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION) //model-vars.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX) //model-vars-tpl.yml
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators())
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators()},
                       DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX) //coab-operators.yml in subdir operators
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                       DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX) //coab-operators.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE},
                        DeploymentConstants.COAB + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION) //coab-vars.yml
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE, "openstack-hws")
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE, "openstack-hws"},
                        "network-operators.yml") //network-operators.yml in subdir openstack-hws
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE, "openstack-hws"},
                        "network-vars.yml") //network-vars.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.TEMPLATE, "openstack-hws"},
                        "network-vars-tpl.yml") //network-vars-tpl.yml
                .build();
    }

    private void assertManifestFile() throws Exception{
        Path expectedManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(expectedManifestFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(expectedManifestFile));
        assertThat(Files.readSymbolicLink(expectedManifestFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION)));

        Path expectedManifestTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(expectedManifestTplFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(expectedManifestTplFile));
        assertThat(Files.readSymbolicLink(expectedManifestTplFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)));

    }

    private void assertVarsFile() throws Exception{
        Path expectedVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(expectedVarsFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(expectedVarsFile));
        assertThat(Files.readSymbolicLink(expectedVarsFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION)));

        Path expectedVarsTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(expectedVarsTplFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(expectedVarsTplFile));
        assertThat(Files.readSymbolicLink(expectedVarsTplFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)));
    }

    private void assertOperatorsFile() throws Exception{
        Path expectedOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedOperatorsFile));
        assertThat("Operators file is not a symbolic link", Files.isSymbolicLink(expectedOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedOperatorsFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX)));
    }

    private void assertIassTypeFile() throws Exception{
        Path expectedIassTypeOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                "openstack-hws",
                "network-operators.yml");
        assertThat("Symbolic link towards iaas type operators file doesn't exist", Files.exists(expectedIassTypeOperatorsFile));
        assertThat("Iaas type operators file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeOperatorsFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                "openstack-hws" + File.separator +
                "network-operators.yml")));

        Path expectedIassTypeVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                "openstack-hws",
                "network-vars.yml");
        assertThat("Symbolic link towards iaas type vars file doesn't exist", Files.exists(expectedIassTypeVarsFile));
        assertThat("Iaas type vars file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeVarsFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeVarsFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                "openstack-hws" + File.separator +
                "network-vars.yml")));

        Path expectedIassTypeVarsTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.TEMPLATE,
                "openstack-hws",
                "network-vars-tpl.yml");
        assertThat("Symbolic link towards iaas type vars file doesn't exist", Files.exists(expectedIassTypeVarsTplFile));
        assertThat("Iaas type vars file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeVarsTplFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeVarsTplFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + DeploymentConstants.TEMPLATE + File.separator +
                "openstack-hws" + File.separator +
                "network-vars-tpl.yml")));
    }

    @Test
    public void check_generation_against_sample_deployment_model() throws IOException {
        //Given a template repository in /tmp
        Path paasTemplatePath = temporaryFolder.getRoot().toPath();

        //Search for the sample-deployment
        Path referenceDataModel = Paths.get("../sample-deployment");

        //Copy reference data model
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Copy.TreeCopier tc = new Copy.TreeCopier(referenceDataModel, paasTemplatePath, "coab-depls", false, true);
        Files.walkFileTree(referenceDataModel, opts, Integer.MAX_VALUE, tc);

        //Check all models
        //checkDeployment("coab-depls", "mongodb", "m");
        //checkDeployment("coab-depls", "cassandravarsops", "c");
        checkDeployment("coab-depls", "cf-mysql", "y");

    }

    private void checkDeployment(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias) throws IOException{

        //Given a path
        Path paasTemplatePath = temporaryFolder.getRoot().toPath();

        //Given and a user request
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator(rootDeployment,
                modelDeployment,
                "operators",
                modelDeploymentShortAlias,
                new VarsFilesYmlFormatter());

        //When
        templatesGenerator.checkPrerequisites(paasTemplatePath);
        templatesGenerator.generate(paasTemplatePath, SERVICE_INSTANCE_ID, coabVarsFileDto);

       //Then
        // assertThat(generatedStructure(), is(equalTo(expectedStructure())));
       System.out.print("Checking : " + modelDeployment);
       String expectedStructure = expectedStructure(rootDeployment, "expected-" + modelDeployment + "-tree.txt", templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
       String generatedStructure = generatedStructure(rootDeployment, modelDeployment, templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID));
       System.out.println(expectedStructure);
       System.out.println(generatedStructure);
       assertEquals(expectedStructure, generatedStructure);
       //System.out.println("=> Success");
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
        Path paasTemplatePath = temporaryFolder.getRoot().toPath();
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
    @Ignore
    public void populateRealPaasTemplates() {

        //Given a path
        Path workDir = Paths.get("/home/losapio/GIT/Coab/paas-templates");

        //Given and a user request
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //Given a template generator
        TemplatesGenerator templatesGenerator = new TemplatesGenerator("coab-depls",
                "cf-mysql",
                "operators",
                "y",
                new VarsFilesYmlFormatter());

        //When
        templatesGenerator.checkPrerequisites(workDir);
        templatesGenerator.generate(workDir, SERVICE_INSTANCE_ID, coabVarsFileDto);

    }

}