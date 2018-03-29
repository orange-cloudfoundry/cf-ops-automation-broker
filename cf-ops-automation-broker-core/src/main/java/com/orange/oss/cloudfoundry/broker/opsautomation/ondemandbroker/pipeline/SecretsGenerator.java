package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;
import java.nio.file.Files;
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
        if (Files.notExists(secretsDir)){
            throw new DeploymentException(DeploymentConstants.SECRETS_EXCEPTION);
        }

        //Check specific pre-requisite (meta file in model secrets directory)
        Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.meta + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(metaFile)){
            throw new DeploymentException(DeploymentConstants.META_FILE_EXCEPTION);
        }

        //Check specific pre-requisite (secrets file in model secrets directory)
        Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.secrets,
                this.secrets + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(secretsFile)){
            throw new DeploymentException(DeploymentConstants.SECRETS_FILE_EXCEPTION);
        }

    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

            //Generate service directory
            super.generate(workDir, serviceInstanceId);

            //Compute instance directory
            String deploymentInstanceDirectory = this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId;

            //Generate secrets directory
            StructureGeneratorHelper.generateDirectory(workDir, this.rootDeployment, deploymentInstanceDirectory, this.secrets);

            //Generate meta file
            String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.secrets};
            String[] targetPathElements = new String[] {this.rootDeployment, deploymentInstanceDirectory, this.secrets};
            String fileName = this.meta + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);

            //Generate secrets file
            fileName = this.secrets + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, fileName, fileName);

            //Generate enable deployment file
            targetPathElements = new String[] {this.rootDeployment, deploymentInstanceDirectory};
            fileName = DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME;
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