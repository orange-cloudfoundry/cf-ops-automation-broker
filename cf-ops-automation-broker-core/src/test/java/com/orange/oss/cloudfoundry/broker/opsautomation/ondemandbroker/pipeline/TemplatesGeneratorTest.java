package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
public class TemplatesGeneratorTest extends StructureGeneratorImplTest{

    @Before
    public void setUp() throws IOException {
        super.setUp();
        this.templatesGenerator = new TemplatesGenerator(this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getTemplate(),
                this.deploymentProperties.getVars(),
                this.deploymentProperties.getOperators(), "c"
        );
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
    public void check_that_template_directory_is_generated() throws IOException {
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
    public void check_that_symlink_towards_manifest_file_is_generated() throws Exception {
        //Given repository, root deployment,model deployment and template directory with model manifest file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateManifestFileSymLink(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards manifest file doesn't exist:" + targetManifestFile, Files.exists(targetManifestFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(targetManifestFile));
        //assertThat(Files.readSymbolicLink(targetManifestFile.toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    @Test
    public void check_that_symlink_towards_manifest_file_are_generated() throws Exception {
        //Given repository, root deployment,model deployment and template directory with model manifest file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateManifestFileSymLinkNew(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetManifestFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards manifest file doesn't exist:" + targetManifestFile, Files.exists(targetManifestFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(targetManifestFile));
        Path targetManifestTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards manifest template file doesn't exist:" + targetManifestTplFile, Files.exists(targetManifestTplFile));
        assertThat("Manifest file is not a symbolic link", Files.isSymbolicLink(targetManifestTplFile));
        //assertThat(Files.readSymbolicLink(targetManifestFile.toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    @Test
    public void check_that_symlink_towards_vars_file_is_generated() throws Exception {
        //Given repository, root deployment,model deployment and template directory with model vars file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateVarsFileSymLink(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(targetVarsFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(targetVarsFile));
        //assertThat(Files.readSymbolicLink(targetVarsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    @Test
    public void check_that_symlink_towards_vars_file_are_generated() throws Exception {
        //Given repository, root deployment,model deployment and template directory with model vars file
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getTemplate()},
                        this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX)
                .build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateVarsFileSymLinkNew(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(targetVarsFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(targetVarsFile));
        Path targetVarsTplFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID) + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        assertThat("Symbolic link towards vars file doesn't exist", Files.exists(targetVarsTplFile));
        assertThat("Vars file is not a symbolic link", Files.isSymbolicLink(targetVarsTplFile));
        //assertThat(Files.readSymbolicLink(targetVarsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
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
        assertThat(Files.readSymbolicLink(targetOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/operators/coab-operators.yml")));
    }

    @Test
    public void check_that_coab_vars_file_is_generated() throws Exception {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),  this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getTemplate())
                .build();

        //When
        this.templatesGenerator.generateCoabVarsFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path coabVarsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.deploymentProperties.getVars() + DeploymentConstants.YML_EXTENSION
        );
        assertThat("Coab vars file doesn't exist", Files.exists(coabVarsFile));
    }

    @Test
    public void check_that_symlinks_towards_operators_files_under_templates_directory_are_generated() throws Exception {

        //Given model and deployment structure
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(),
                                this.deploymentProperties.getModelDeployment(),
                                this.deploymentProperties.getTemplate()},
                        "1-add-shield-operators.yml").build();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),
                                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                                this.deploymentProperties.getTemplate()).build();

        //When
        this.templatesGenerator.generateOperatorsFileSymLinks(workDir, SERVICE_INSTANCE_ID);

        //Then
        Path expectedFirstOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "1-add-shield-operators.yml");
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedFirstOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(expectedFirstOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedFirstOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/template/1-add-shield-operators.yml")));

    }

    @Test
    public void check_that_all_symlinks_templates_directory_are_generated() throws Exception {

        //Given model and deployment structure
        aModelStructure();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(),
                        this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                        this.deploymentProperties.getTemplate()).build();


        //When
        this.templatesGenerator.generateAllSymLinks(workDir, SERVICE_INSTANCE_ID);
        //Then
        Path expectedFirstOperatorsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.templatesGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getTemplate(),
                "coab-operators.yml");
        assertThat("Symbolic link towards operators file doesn't exist", Files.exists(expectedFirstOperatorsFile));
        assertThat("Coab operators file is not a symbolic link", Files.isSymbolicLink(expectedFirstOperatorsFile));
        assertThat(Files.readSymbolicLink(expectedFirstOperatorsFile).toString(), is(equalTo("../../" + this.deploymentProperties.getModelDeployment() + "/template/coab-operators.yml")));

    }









    @Test
    public void check_that_only_manifest_files_are_found() {
        //Given model structure with files
        this.aModelStructure();

        //When
        List<String> manifestList= this.templatesGenerator.searchForManifestFiles(this.workDir);

        //Then
        assertThat("model.yml is not present", manifestList.contains(this.deploymentProperties.getModelDeployment() + DeploymentConstants.YML_EXTENSION));
        assertThat("model-tpl.yml is not present", manifestList.contains(this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX));

    }

    @Test
    public void check_that_only_vars_files_are_found() {
        //Given
        this.aModelStructure();

        //When
        List<String> varsList= this.templatesGenerator.searchForVarsFiles(this.workDir);

        //Then
        assertThat("model-vars.yml is not present", varsList.contains(this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION));
        assertThat("model-vars-tpl.yml is not present", varsList.contains(this.deploymentProperties.getModelDeployment() + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX));

    }

    @Test
    public void check_that_only_operators_files_are_found() {
        //Given
        this.aModelStructure();

        //When
        List<String> operatorsList= this.templatesGenerator.searchForOperatorsFiles(this.workDir);

        //Then
        assertThat("coab-operators.yml is not present", operatorsList.contains(DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX));

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
                .build();
    }




    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void populatePaasTemplates() {
        Path workDir = Paths.get("/home/losapio/GIT/Coab/paas-templates/");
        this.templatesGenerator.checkPrerequisites(workDir);
        this.templatesGenerator.generate(workDir, SERVICE_INSTANCE_ID);
    }
}