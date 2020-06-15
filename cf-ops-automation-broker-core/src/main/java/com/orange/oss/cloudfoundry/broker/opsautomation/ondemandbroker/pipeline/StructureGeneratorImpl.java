package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;

public class StructureGeneratorImpl implements StructureGenerator {

    String rootDeployment;
    String modelDeployment;
    String modelDeploymentShortAlias;
    String modelDeploymentSeparator;

    public StructureGeneratorImpl(){
    }

    public StructureGeneratorImpl(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias, String modelDeploymentSeparator){
        this.rootDeployment = rootDeployment;
        this.modelDeployment = modelDeployment;
        this.modelDeploymentShortAlias = modelDeploymentShortAlias;
        this.modelDeploymentSeparator = modelDeploymentSeparator;
    }

    public void checkPrerequisites(Path workDir) {

        this.checkThatRootDeploymentExists(workDir);

        this.checkThatModelDeploymentExists(workDir);
    }

    protected void checkThatRootDeploymentExists(Path workDir){
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment);
        if (StructureGeneratorHelper.isMissingResource(rootDeploymentDir)){
            throw new DeploymentException(DeploymentConstants.ROOT_DEPLOYMENT_EXCEPTION + rootDeploymentDir);
        }
    }

    protected void checkThatModelDeploymentExists(Path workDir){
        Path modelDeploymentDir = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment);
        if (StructureGeneratorHelper.isMissingResource(modelDeploymentDir)){
            throw new DeploymentException(DeploymentConstants.MODEL_DEPLOYMENT_EXCEPTION + modelDeploymentDir);
        }
    }

    public void generate(Path workDir, String serviceInstanceId, CoabVarsFileDto coabVarsFileDto) {
            ////Generate service directory
            StructureGeneratorHelper.generateDirectory(workDir, this.rootDeployment, this.computeDeploymentName(serviceInstanceId));
    }

    public String computeDeploymentName(String serviceInstanceId){
        return this.modelDeploymentShortAlias + this.modelDeploymentSeparator + serviceInstanceId;
    }

}
