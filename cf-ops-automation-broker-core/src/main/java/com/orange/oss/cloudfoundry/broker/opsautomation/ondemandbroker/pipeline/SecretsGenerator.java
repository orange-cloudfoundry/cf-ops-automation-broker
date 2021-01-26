package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

public class SecretsGenerator extends StructureGeneratorImpl implements SecretsReader {

    private static Logger logger = LoggerFactory.getLogger(SecretsGenerator.class.getName());

    private final VarsFilesYmlFormatter varsFilesYmlFormatter;

    public SecretsGenerator(String rootDeployment, String modelDeployment, String modelDeploymentShortAlias,
        String modelDeploymentSeparator, VarsFilesYmlFormatter varsFilesYmlFormatter){
        super(rootDeployment,modelDeployment, modelDeploymentShortAlias, modelDeploymentSeparator);
        this.varsFilesYmlFormatter = varsFilesYmlFormatter;
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
                this.modelDeploymentShortAlias + this.modelDeploymentSeparator  + serviceInstanceId,
                this.modelDeploymentShortAlias + this.modelDeploymentSeparator + serviceInstanceId + DeploymentConstants.YML_EXTENSION);
    }

    @Override
    public boolean isBoshDeploymentAvailable(Path secretsWorkDir, String serviceInstanceId) {
        Path targetManifestFile = getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        boolean exists = !StructureGeneratorHelper.isMissingResource(targetManifestFile);
        logger.debug("Manifest at path {} exists: {}", targetManifestFile, exists);
        return exists;
    }

    @Override
    public CoabVarsFileDto getBoshDeploymentCompletionMarker(Path secretsWorkDir, String serviceInstanceId)
        throws IOException {
        Assert.isTrue(isBoshDeploymentAvailable(secretsWorkDir, serviceInstanceId), "completion marker can't be " +
            "fetched when deployment manifest missing");
        Path targetManifestFile = getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        return varsFilesYmlFormatter.parseFromBoshManifestYml(targetManifestFile);
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
    protected boolean isEnableDeploymentFileIsPresent(Path workDir, String serviceInstanceId){
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        //Compute target path
        Path targetDir = StructureGeneratorHelper.generatePath(workDir, targetPathElements);
        Path targetFile = StructureGeneratorHelper.generatePath(targetDir, fileName);


        boolean isMarkerPresent = !StructureGeneratorHelper.isMissingResource(targetFile);
        if (!isMarkerPresent) {
            logger.warn("enable-deployment.yml is missing at " + targetFile);
        }
        return isMarkerPresent;
    }

    protected void removeEnableDeploymentFile(Path workDir, String serviceInstanceId) {
        String[] pathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

}