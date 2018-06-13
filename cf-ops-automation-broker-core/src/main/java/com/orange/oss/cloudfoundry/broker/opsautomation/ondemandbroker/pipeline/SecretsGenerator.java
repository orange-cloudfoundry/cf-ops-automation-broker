package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SecretsGenerator extends StructureGeneratorImpl implements SecretsReader {

    private static Logger logger = LoggerFactory.getLogger(SecretsGenerator.class.getName());
    private String secrets;
    private String meta;

    public SecretsGenerator(){
    }

    public SecretsGenerator(String rootDeployment, String modelDeployment, String secrets, String meta, String modelDeploymentShortAlias){
        super(rootDeployment,modelDeployment, modelDeploymentShortAlias);
        this.secrets = secrets;
        this.meta = meta;
    }

    @Override
    public void checkPrerequisites(Path workDir) {
        //Check common pre-requisites
        super.checkPrerequisites(workDir);

        //Check specific pre-requisite (secrets directory)
        this.checkThatSecretsDirectoryExists(workDir);

        //Check specific pre-requisite (meta file in model secrets directory)
        this.checkThatMetaFileExists(workDir);

        //Check specific pre-requisite (secrets file in model secrets directory)
        this.checkThatSecretsFileExists(workDir);
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

        //Generate service directory
        super.generate(workDir, serviceInstanceId);

        //Generate secrets directory
        this.generateSecretsDirectory(workDir, serviceInstanceId);

        //Generate meta file
        this.generateMetaFile(workDir, serviceInstanceId);

        //Generate secrets file
        this.generateSecretsFile(workDir, serviceInstanceId);

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
        boolean exists = StructureGeneratorHelper.isMissingResource(targetManifestFile) ? false : true;
        logger.debug("Manifest at path {} exists: {}", targetManifestFile, exists);
        return exists;
    }

    public void remove(Path workDir, String serviceInstanceId) {

        //Remove meta file
        this.removeMetaFile(workDir, serviceInstanceId);

        //Remove secrets file
        this.removeSecretsFile(workDir, serviceInstanceId);

        //Remove enable deployment file
        this.removeEnableDeploymentFile(workDir, serviceInstanceId);

    }

    protected void checkThatSecretsDirectoryExists(Path workDir){
        Path secretsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets);
        if (StructureGeneratorHelper.isMissingResource(secretsDir)){
            throw new DeploymentException(DeploymentConstants.SECRETS_EXCEPTION);
        }
    }

    protected void checkThatMetaFileExists(Path workDir){
        Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.meta + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(metaFile)){
            throw new DeploymentException(DeploymentConstants.META_FILE_EXCEPTION);
        }
    }

    protected void checkThatSecretsFileExists(Path workDir){
        Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.secrets + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(secretsFile)){
            throw new DeploymentException(DeploymentConstants.SECRETS_FILE_EXCEPTION);
        }
    }

    protected void generateSecretsDirectory(Path workDir, String serviceInstanceId){
        StructureGeneratorHelper.generateDirectory(workDir,
                this.rootDeployment,
                this.computeDeploymentName(serviceInstanceId),
                this.secrets);
    }

    protected void generateMetaFile(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.secrets};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.secrets};
        String fileName = this.meta + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);
    }

    protected void generateSecretsFile(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.secrets};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.secrets};
        String fileName = this.secrets + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);
    }

    protected void generateEnableDeploymentFile(Path workDir, String serviceInstanceId){
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.generateFile(workDir,  targetPathElements, fileName, fileName, null);
    }

    protected void removeMetaFile(Path workDir, String serviceInstanceId) {
        String[] pathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.secrets};
        String fileName = this.meta + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

    protected void removeSecretsFile(Path workDir, String serviceInstanceId) {
        String[] pathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.secrets};
        String fileName = this.secrets + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

    protected void removeEnableDeploymentFile(Path workDir, String serviceInstanceId) {
        String[] pathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

}