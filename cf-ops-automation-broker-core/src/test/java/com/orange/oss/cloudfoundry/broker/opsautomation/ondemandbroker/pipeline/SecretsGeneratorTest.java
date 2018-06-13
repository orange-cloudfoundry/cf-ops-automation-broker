package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

//$ tree coab-depls
//coab-depls
//|-- cassandravarsops
//|   |-- cassandravarsops.yml
//|   |-- enable-deployment.yml
//|   `-- secrets
//|       |-- meta.yml
//|       `-- secrets.yml
//|-- c_155cf3e0-6321-48de-9aac-4dd132baf21f
//|   |-- c_155cf3e0-6321-48de-9aac-4dd132baf21f.yml
//|   |-- enable-deployment.yml
//|   `-- secrets
//|       |-- meta.yml -> ../../cassandravarsops/secrets/meta.yml
//|       `-- secrets.yml -> ../../cassandravarsops/secrets/secrets.yml
//|[..]
public class SecretsGeneratorTest extends StructureGeneratorImplTest{

    private SecretsGenerator secretsGenerator;

    @Before
    public void setUp() throws IOException {
        super.setUp();
        this.secretsGenerator = new SecretsGenerator(this.deploymentProperties.getRootDeployment(),
                                    this.deploymentProperties.getModelDeployment(),
                                    this.deploymentProperties.getSecrets(),
                                    this.deploymentProperties.getMeta(),
                                    this.deploymentProperties.getModelDeploymentShortAlias());
    }

    @Test
    public void raise_exception_if_root_deployment_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);

        //Given initialized by setUp method

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);
    }//superclass TU

    @Test
    public void raise_exception_if_model_deployment_directory_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);

        //Given (a part is initialized by setUp method)
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment())
                .build();

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);
    }//superclass TU

    @Test
    public void raise_exception_if_secrets_directory_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.SECRETS_EXCEPTION);

        //When
        this.secretsGenerator.checkThatSecretsDirectoryExists(this.workDir);
    }

    @Test
    public void raise_exception_if_model_meta_file_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.META_FILE_EXCEPTION);

        //When
        this.secretsGenerator.checkThatMetaFileExists(this.workDir);
    }

    @Test
    public void raise_exception_if_model_secrets_file_is_missing() {
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.SECRETS_FILE_EXCEPTION);

        //When
        this.secretsGenerator.checkThatSecretsFileExists(this.workDir);
    }

    @Test
    public void check_that_all_prerequisites_are_satisfied() {
        //Given
        this.aModelStructure();

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);

        //Then => No exception raised
    }

    @Test
    public void check_that_secrets_directory_is_generated() {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID))
                .build();

        //When
        this.secretsGenerator.generateSecretsDirectory(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path secretsDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getSecrets()
        );
        assertThat("Secrets directory doesn't exist:" + secretsDir, Files.exists(secretsDir));
    }

    @Test
    public void check_that_meta_file_is_generated() {
        //Given
        this.aModelStructure();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getSecrets())
                .build();

        //When
        this.secretsGenerator.generateMetaFile(workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetMetaFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
        assertThat("Meta file doesn't exist :"+ targetMetaFile, Files.exists(targetMetaFile));
        assertThat("Meta file is not a symbolic link", Files.isSymbolicLink(targetMetaFile));
    }

    @Test
    public void check_that_secrets_file_is_generated() {
        //Given
        this.aModelStructure();
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getSecrets())
                .build();

        //When
        this.secretsGenerator.generateSecretsFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetSecretsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
        assertThat("Secrets file doesn't exist:" + targetSecretsFile, Files.exists(targetSecretsFile));
        assertThat("Secrets file is not a symbolic link", Files.isSymbolicLink(targetSecretsFile));
    }

    @Test
    public void check_that_enable_deployment_file_is_generated() {
        //Given
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID))
                .build();

        //When
        this.secretsGenerator.generateEnableDeploymentFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetEnableDeploymentFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME);
        assertThat("Enable deployment file doesn't exist:" + targetEnableDeploymentFile, Files.exists(targetEnableDeploymentFile));
    }

    @Test
    public void check_that_coa_produced_manifest_is_parametrized()  {
        //Given a DeploymentProperties for cassandra with "m" prefix (e.g. mongo)
        secretsGenerator = new SecretsGenerator("coab-depls",
                "cassandravarsops",
                "secrets",
                "meta",
                "m");


        //When
        Path targetManifestFilePath = secretsGenerator.getTargetManifestFilePath(this.workDir, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10");

        //Then
        Path expectedCoaGeneratedManifestFile = this.workDir.resolve("coab-depls/m_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10/m_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10.yml");

        assertThat("manifest file location", targetManifestFilePath, equalTo(expectedCoaGeneratedManifestFile));
    }



    @Test
    public void check_that_meta_file_is_removed() {

        //Given
        this.aDeploymentStructure();

        //When
        this.secretsGenerator.removeMetaFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetMetaFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID),
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION);
        assertThat("Meta file is still existing", Files.notExists(targetMetaFile));
    }

    @Test
    public void check_that_secrets_file_is_removed() {
        //Given
        this.aDeploymentStructure();

        //When
        this.secretsGenerator.removeSecretsFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetSecretsFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                this.deploymentProperties.getSecrets(),
                this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION);
        assertThat("Secrets file is still existing", Files.notExists(targetSecretsFile));
    }

    @Test
    public void check_that_enable_deployment_file_is_removed() throws IOException {

        //Given
        this.aDeploymentStructure();

        //When
        this.secretsGenerator.removeEnableDeploymentFile(this.workDir, SERVICE_INSTANCE_ID);

        //Then
        Path targetEnableDeploymentFile = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.deploymentProperties.getModelDeployment() +  DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID,
                DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME);
        assertThat("Enable deployment file is still existing", Files.notExists(targetEnableDeploymentFile));

    }

    private void aModelStructure(){
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getSecrets()},
                        this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION) //meta.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getSecrets()},
                        this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION) //secrets.yml
                .build();
    }

    private void aDeploymentStructure(){
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getSecrets()},
                        this.deploymentProperties.getMeta() + DeploymentConstants.YML_EXTENSION) //meta.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), this.deploymentProperties.getSecrets()},
                        this.deploymentProperties.getSecrets() + DeploymentConstants.YML_EXTENSION) //secrets.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID)},
                        DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME) //enable-deployment.yml
                .build();
    }


    @Test
    @Ignore
    public void check_if_files_content_are_correct() {
        //TODO
    }

    @Test
    @Ignore
    public void populatePaasSecrets() {
        Path workDir = Paths.get("/home/ijly7474/GIT/coab/preprod-secrets");
        this.secretsGenerator.checkPrerequisites(workDir);
        this.secretsGenerator.generate(workDir, SERVICE_INSTANCE_ID);
    }

}
