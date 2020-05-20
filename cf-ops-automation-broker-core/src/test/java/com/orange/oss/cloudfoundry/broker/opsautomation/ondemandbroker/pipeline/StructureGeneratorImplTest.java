package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StructureGeneratorImplTest {

    protected static final String REPOSITORY_DIRECTORY = "paas";
    protected static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10";
    protected DeploymentProperties deploymentProperties;
    protected File file;
    protected Path workDir;

    private StructureGeneratorImpl structureGeneratorImpl;

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        this.deploymentProperties = aDeploymentProperties();
        this.file = tempDir.toPath().resolve(REPOSITORY_DIRECTORY).toFile();
        //noinspection ResultOfMethodCallIgnored
        this.file.mkdir();
        this.workDir = file.toPath();
        this.structureGeneratorImpl = new StructureGeneratorImpl(this.deploymentProperties.getRootDeployment(), this.deploymentProperties.getModelDeployment(), this.deploymentProperties.getModelDeploymentShortAlias());
    }

    @Test
    public void raise_exception_if_root_deployment_is_missing(){
        DeploymentException deploymentException = assertThrows(DeploymentException.class,
            () ->
        //When
                this.structureGeneratorImpl.checkThatRootDeploymentExists(this.workDir));
        //Then
        assertThat(deploymentException).hasMessageStartingWith(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);
    }

    @Test
    public void raise_exception_if_model_deployment_is_missing(){
        DeploymentException deploymentException = assertThrows(DeploymentException.class,
            () ->
        //When
                this.structureGeneratorImpl.checkThatModelDeploymentExists(this.workDir));
        //Then
        assertThat(deploymentException).hasMessageStartingWith(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);
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
        assertThat(Files.exists(deploymentInstanceDir)).as("Deployment directory doesn't exist: " + deploymentInstanceDir).isTrue();
    }

    private DeploymentProperties aDeploymentProperties() {
        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setRootDeployment("coab-depls");
        deploymentProperties.setModelDeployment("mongodb");
        deploymentProperties.setModelDeploymentShortAlias("m_");
        deploymentProperties.setBrokerDisplayName("mongo db");
        return deploymentProperties;
    }





}
