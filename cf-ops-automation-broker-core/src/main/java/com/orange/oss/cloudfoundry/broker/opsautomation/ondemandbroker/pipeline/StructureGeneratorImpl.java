package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by ijly7474 on 15/12/17.
 */
public class StructureGeneratorImpl implements StructureGenerator {

    String rootDeployment;
    String modelDeployment;
    String modelDeploymentShortAlias;

    public StructureGeneratorImpl(){
    }

    public StructureGeneratorImpl(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias){
        this.rootDeployment = rootDeployment;
        this.modelDeployment = modelDeployment;
        this.modelDeploymentShortAlias = modelDeploymentShortAlias;
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
            StructureGeneratorHelper.generateDirectory(workDir, this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId));
    }

    public String computeDeploymentInstance(String serviceInstanceId){
        return this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId;
    }

}
