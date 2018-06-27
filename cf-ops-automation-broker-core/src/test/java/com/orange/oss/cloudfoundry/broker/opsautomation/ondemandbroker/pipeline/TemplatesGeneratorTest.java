package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
    public void setUp() throws IOException {
        super.setUp();
        this.templatesGenerator = new TemplatesGenerator(this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getVars(),
                this.deploymentProperties.getOperators(),
                "c",
                new VarsFilesYmlFormatter());
    }

    private TemplatesGenerator templatesGenerator;

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
                this.deploymentProperties.getTemplate()
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
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //When
        this.templatesGenerator.generateCoabVarsFile(this.workDir, SERVICE_INSTANCE_ID, coabVarsFileDto);

        //Then
        Path coabVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION
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
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateCoabOperatorsFileSymLink(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
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
                        this.deploymentProperties.getTemplate()).build();


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
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate())
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION) //model.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX) //model-tpl.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION) //model-vars.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX) //model-vars-tpl.yml
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators())
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getOperators()},
                       DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX) //coab-operators.yml in subdir operators
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                       DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX) //coab-operators.yml
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate(), "openstack-hws")
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate(), "openstack-hws"},
                        "network-operators.yml") //network-operators.yml in subdir openstack-hws
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate(), "openstack-hws"},
                        "network-vars.yml") //network-vars.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate(), "openstack-hws"},
                        "network-vars-tpl.yml") //network-vars-tpl.yml
                .build();
    }

    private void assertManifestFile() throws Exception{
        Path expectedManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(expectedManifestFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(expectedManifestFile));
        assertThat(Files.readSymbolicLink(expectedManifestFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION)));

        Path expectedManifestTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards manifest file doesn't exist", Files.exists(expectedManifestTplFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(expectedManifestTplFile));
        assertThat(Files.readSymbolicLink(expectedManifestTplFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)));

    }

    private void assertVarsFile() throws Exception{
        Path expectedVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(expectedVarsFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(expectedVarsFile));
        assertThat(Files.readSymbolicLink(expectedVarsFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION)));

        Path expectedVarsTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(expectedVarsTplFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(expectedVarsTplFile));
        assertThat(Files.readSymbolicLink(expectedVarsTplFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)));
    }

    private void assertOperatorsFile() throws Exception{
        Path expectedOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedOperatorsFile));
        assertThat("Operators file is not a symbolic link", Files.isSymbolicLink(expectedOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedOperatorsFile).toString(), is(equalTo(SYM_LINK +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX)));
    }

    private void assertIassTypeFile() throws Exception{
        Path expectedIassTypeOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "openstack-hws",
                "network-operators.yml");
        assertThat("Symbolic link towards iaas type operators file doesn't exist", Files.exists(expectedIassTypeOperatorsFile));
        assertThat("Iaas type operators file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeOperatorsFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                "openstack-hws" + File.separator +
                "network-operators.yml")));

        Path expectedIassTypeVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "openstack-hws",
                "network-vars.yml");
        assertThat("Symbolic link towards iaas type vars file doesn't exist", Files.exists(expectedIassTypeVarsFile));
        assertThat("Iaas type vars file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeVarsFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeVarsFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                "openstack-hws" + File.separator +
                "network-vars.yml")));

        Path expectedIassTypeVarsTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "openstack-hws",
                "network-vars-tpl.yml");
        assertThat("Symbolic link towards iaas type vars file doesn't exist", Files.exists(expectedIassTypeVarsTplFile));
        assertThat("Iaas type vars file is not a symbolic link", Files.isSymbolicLink(expectedIassTypeVarsTplFile));
        assertThat(Files.readSymbolicLink(expectedIassTypeVarsTplFile).toString(), is(equalTo("../../../" +
                this.deploymentProperties.getModelDeployment() +
                File.separator + this.deploymentProperties.getTemplate() + File.separator +
                "openstack-hws" + File.separator +
                "network-vars-tpl.yml")));
    }



    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void populatePaasTemplates() throws URISyntaxException {
        //Given a template repository in /tmp
        String paasTemplatePath = temporaryFolder.getRoot().getAbsolutePath();

        //Search from classpath the test repository using a file marker  /SampleModelTestGenerator/
        URL resource = this.getClass().getResource("/sample-deployment-model");
        Path referenceDataModel = Paths.get(resource.toURI());

        //Recursive copy reference template
            //Files.walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth, FileVisitor<? super Path> visitor) throws IOException
                //Files.copy(source, target, options)

        //Given and a user request
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();

        //When
        this.templatesGenerator.checkPrerequisites(workDir);
        this.templatesGenerator.generate(workDir, SERVICE_INSTANCE_ID, coabVarsFileDto);

        //then the service instance looks like the expected reference model


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

}