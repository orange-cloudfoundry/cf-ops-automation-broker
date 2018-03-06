package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ijly7474 on 15/12/17.
 */
public class StructureGeneratorImpl implements StructureGenerator {

    protected String rootDeployment;
    protected String modelDeployment;

    public StructureGeneratorImpl(){
    }

    public StructureGeneratorImpl(String rootDeployment, String modelDeployment){
        this.rootDeployment = rootDeployment;
        this.modelDeployment = modelDeployment;
    }

    public void checkPrerequisites(Path workDir) {
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment);
        if (Files.notExists(rootDeploymentDir)){
            throw new DeploymentException(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION);
        }
        Path modelDeploymentDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment);
        if (Files.notExists(modelDeploymentDir)){
            throw new DeploymentException(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION);
        }
    }

    public void generate(Path workDir, String serviceInstanceId) {
            ////Generate service directory
            String deploymentInstanceDirectory = this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId;
            StructureGeneratorHelper.generateDirectory(workDir, this.rootDeployment, deploymentInstanceDirectory);
    }

}
