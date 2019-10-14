package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SecretsGenerator extends StructureGeneratorImpl implements SecretsReader {

    private static Logger logger = LoggerFactory.getLogger(SecretsGenerator.class.getName());

    public SecretsGenerator(){
    }

    public SecretsGenerator(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias){
        super(rootDeployment,modelDeployment, modelDeploymentShortAlias);
    }

    @Override
    public void checkPrerequisites(Path workDir) {
        //Check common pre-requisites
        super.checkPrerequisites(workDir);
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId, CoabVarsFileDto coabVarsFileDto) {

        //Generate service directory
        super.generate(workDir, serviceInstanceId, coabVarsFileDto);

        //Generate enable deployment file
        this.generateEnableDeploymentFile(workDir, serviceInstanceId);
    }

    //
    // SecretsReader impl
    //

    @Override
    public Path getTargetManifestFilePath(Path workDir, String serviceInstanceId) {
        return StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE  + serviceInstanceId,
                this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId + DeploymentConstants.YML_EXTENSION);
    }

    @Override
    public boolean isBoshDeploymentAvailable(Path secretsWorkDir, String serviceInstanceId) {
        Path targetManifestFile = getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        boolean exists = !StructureGeneratorHelper.isMissingResource(targetManifestFile);
        logger.debug("Manifest at path {} exists: {}", targetManifestFile, exists);
        return exists;
    }

    public void remove(Path workDir, String serviceInstanceId) {

        //Remove enable deployment file
        this.removeEnableDeploymentFile(workDir, serviceInstanceId);

    }

    protected void generateEnableDeploymentFile(Path workDir, String serviceInstanceId){
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.generateFile(workDir,  targetPathElements, fileName, fileName, null);
    }

    protected void removeEnableDeploymentFile(Path workDir, String serviceInstanceId) {
        String[] pathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

}