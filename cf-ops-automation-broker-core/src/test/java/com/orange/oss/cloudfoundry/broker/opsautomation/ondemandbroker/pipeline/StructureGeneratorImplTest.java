package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class StructureGeneratorImplTest {

    protected static final String REPOSITORY_DIRECTORY = "paas";
    protected static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
    protected DeploymentProperties deploymentProperties;
    protected File file;
    protected Path workDir;

    private StructureGeneratorImpl structureGeneratorImpl;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        this.deploymentProperties = aDeploymentProperties();
        this.file = temporaryFolder.newFolder(REPOSITORY_DIRECTORY);
        this.workDir = file.toPath();
        this.structureGeneratorImpl = new StructureGeneratorImpl(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getModelDeploymentShortAlias());
    }

    @Test
    public void raise_exception_if_root_deployment_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);

        //When
        this.structureGeneratorImpl.checkThatRootDeploymentExists(this.workDir);
    }

    @Test
    public void raise_exception_if_model_deployment_is_missing(){
        //Then
        thrown.expect(DeploymentException.class);
        thrown.expectMessage(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);

        //When
        this.structureGeneratorImpl.checkThatModelDeploymentExists(this.workDir);
    }

    @Test
    public void check_that_all_prerequisites_are_satisfied() {
        //Given a model structure that meets all prerequisites
        Structure modelStructure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment())
                .build();

        //When
        this.structureGeneratorImpl.checkPrerequisites(this.workDir);
    }

    @Test
    public void check_deployment_instance_computation() {
        //Given initialized by setUp method

        //When
        String deploymentInstance = this.structureGeneratorImpl.computeDeploymentName(SERVICE_INSTANCE_ID);

        //Then
        String expectedDeploymentInstance = this.deploymentProperties.getModelDeploymentShortAlias() + DeploymentConstants.UNDERSCORE + SERVICE_INSTANCE_ID;
        assertEquals(expectedDeploymentInstance, deploymentInstance);

    }

    @Test
    public void check_that_deployment_instance_directory_is_generated() {
        //Given
        Structure structure = new Structure.StructureBuilder(this.workDir)
                .withDirectoryHierarchy(this.deploymentProperties.getRootDeployment())
                .build();

        //When
        this.structureGeneratorImpl.generate(this.workDir, SERVICE_INSTANCE_ID, null);

        //Then
        Path deploymentInstanceDir = StructureGeneratorHelper.generatePath(this.workDir,
                this.deploymentProperties.getRootDeployment(),
                this.structureGeneratorImpl.computeDeploymentName(SERVICE_INSTANCE_ID));
        assertThat("Deployment directory doesn't exist: " + deploymentInstanceDir, Files.exists(deploymentInstanceDir));
    }

    private DeploymentProperties aDeploymentProperties() {
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("mongodb");
        deploymentProperties.setTemplate("template");
        deploymentProperties.setVars("vars");
        deploymentProperties.setOperators("operators");
        deploymentProperties.setMeta("meta");
        deploymentProperties.setSecrets("secrets");
        return deploymentProperties;
    }





}
