package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import java.nio.file.Path;

public class SecretsGenerator extends StructureGeneratorImpl {

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
        Path secretsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets);
        if (StructureGeneratorHelper.isMissingResource(secretsDir)){
            throw new DeploymentException(DeploymentConstants.SECRETS_EXCEPTION);
        }

        //Check specific pre-requisite (meta file in model secrets directory)
        Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.meta + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(metaFile)){
            throw new DeploymentException(DeploymentConstants.META_FILE_EXCEPTION);
        }

        //Check specific pre-requisite (secrets file in model secrets directory)
        Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.secrets + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(secretsFile)){
            throw new DeploymentException(DeploymentConstants.SECRETS_FILE_EXCEPTION);
        }
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

        //Generate service directory
        super.generate(workDir, serviceInstanceId);

œœ        //Generate secrets directory
        this.generateSecretsDirectory(workDir, serviceInstanceId);

        //Generate meta file
        this.generateMetaFile(workDir, serviceInstanceId);

        //Generate secrets file
        this.generateSecretsFile(workDir, serviceInstanceId);

        //Generate enable deployment file
        this.generateEnableDeploymentFile(workDir, serviceInstanceId);
    }

    private void generateSecretsDirectory(Path workDir, String serviceInstanceId){
        Path deploymentSecretsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.computeDeploymentInstance(serviceInstanceId),
                this.secrets);
        StructureGeneratorHelper.generateDirectory(deploymentSecretsDir, null);
    }

    private void generateMetaFile(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.secrets};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.secrets};
        String fileName = this.meta + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);
    }

    private void generateSecretsFile(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.secrets};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.secrets};
        String fileName = this.secrets + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);
    }

    private void generateEnableDeploymentFile(Path workDir, String serviceInstanceId){
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId)};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.generateFile(workDir,  targetPathElements, fileName, fileName, null);
    }

    public void remove(Path workDir, String serviceInstanceId) {

            //Compute instance directory
            String deploymentInstanceDirectory = this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId;

            //Remove meta file
            this.removeMetaFile(workDir, deploymentInstanceDirectory);

            //Remove secrets file
            this.removeSecretsFile(workDir, deploymentInstanceDirectory);

            //Remove enable deployment file
            this.removeEnableDeploymentFile(workDir, deploymentInstanceDirectory);

    }

    private void removeMetaFile(Path workDir, String deploymentInstanceDirectory) {
        String[] pathElements = new String[] {this.rootDeployment, deploymentInstanceDirectory, this.secrets};
        String fileName = this.meta + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

    private void removeSecretsFile(Path workDir, String deploymentInstanceDirectory) {
        String[] pathElements = new String[] {this.rootDeployment, deploymentInstanceDirectory, this.secrets};
        String fileName = this.secrets + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

    private void removeEnableDeploymentFile(Path workDir, String deploymentInstanceDirectory) {
        String[] pathElements = new String[] {this.rootDeployment, deploymentInstanceDirectory};
        String fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
        StructureGeneratorHelper.removeFile(workDir, pathElements, fileName);
    }

}