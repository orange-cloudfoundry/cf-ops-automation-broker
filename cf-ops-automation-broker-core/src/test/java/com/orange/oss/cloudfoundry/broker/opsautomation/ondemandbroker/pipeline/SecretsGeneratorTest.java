package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

//$ tree coab-depls
//coab-depls
//|-- cassandra
//|   |-- cassandra.yml
//|   |-- enable-deployment.yml
//|   `-- secrets
//|       |-- meta.yml
//|       `-- secrets.yml
//|-- c_155cf3e0-6321-48de-9aac-4dd132baf21f
//|   |-- c_155cf3e0-6321-48de-9aac-4dd132baf21f.yml
//|   |-- enable-deployment.yml
//|[..]
public class SecretsGeneratorTest extends StructureGeneratorImplTest{

    private SecretsGenerator secretsGenerator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.secretsGenerator = new SecretsGenerator(this.deploymentProperties.getRootDeployment(),
                                    this.deploymentProperties.getModelDeployment(),
                this.deploymentProperties.getModelDeploymentShortAlias());
    }

    @Test
    public void raise_exception_if_root_deployment_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(startsWith(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION));

        //Given initialized by setUp method

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);
    }//superclass TU

    @Test
    public void raise_exception_if_model_deployment_directory_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(startsWith(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION));

        //Given (a part is initialized by setUp method)
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment())
                .build();

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);
    }//superclass TU

    @Test
    public void check_that_all_prerequisites_are_satisfied() {
        //Given
        this.aModelStructure();

        //When
        this.secretsGenerator.checkPrerequisites(this.workDir);

        //Then => No exception raised
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
                "m");


        //When
        Path targetManifestFilePath = secretsGenerator.getTargetManifestFilePath(this.workDir, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10");

        //Then
        Path expectedCoaGeneratedManifestFile = this.workDir.resolve("coab-depls/m_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10/m_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10.yml");

        assertThat("manifest file location", targetManifestFilePath, equalTo(expectedCoaGeneratedManifestFile));
    }

    private void aModelStructure(){
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.SECRETS},
                        DeploymentConstants.META + DeploymentConstants.YML_EXTENSION) //meta.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), DeploymentConstants.SECRETS},
                        DeploymentConstants.SECRETS + DeploymentConstants.YML_EXTENSION) //secrets.yml
                .build();
    }

    private void aDeploymentStructure(){
        Structure deploymentStructure = new Structure.StructureBuilder(this.workDir)
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), DeploymentConstants.SECRETS},
                        DeploymentConstants.META + DeploymentConstants.YML_EXTENSION) //meta.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID), DeploymentConstants.SECRETS},
                        DeploymentConstants.SECRETS + DeploymentConstants.YML_EXTENSION) //secrets.yml
                .withFile(new String[]{this.deploymentProperties.getRootDeployment(), this.secretsGenerator.computeDeploymentName(SERVICE_INSTANCE_ID)},
                        DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME) //enable-deployment.yml
                .build();
    }

    @Test
    @Ignore
    public void populatePaasSecrets() {
        Path workDir = Paths.get("/home/ijly7474/GIT/coab/preprod-secrets");
        this.secretsGenerator.checkPrerequisites(workDir);
        this.secretsGenerator.generate(workDir, SERVICE_INSTANCE_ID, null);
    }

}
