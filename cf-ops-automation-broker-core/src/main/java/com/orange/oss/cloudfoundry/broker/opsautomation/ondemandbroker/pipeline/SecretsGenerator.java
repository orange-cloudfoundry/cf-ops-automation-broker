package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by ijly7474 on 14/12/17.
 */
public class SecretsGenerator extends StructureGeneratorImpl {

    private String secrets;
    private String meta;
    private String enable;

    public SecretsGenerator(){
    }

    public SecretsGenerator(String rootDeployment, String modelDeployment, String secrets, String meta){
        super(rootDeployment,modelDeployment);
        this.secrets = secrets;
        this.meta = meta;
    }

    public void generate(Path workDir, String serviceInstanceId) {

            //Generate service directory
            super.generate(workDir, serviceInstanceId);

            //Generate secrets directory
            String deploymentInstanceDirectory = this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId;
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
            //StructureGeneratorHelper.generateFile(workDir,targetPathElements, fileName, DeploymentConstants.YML_HEADER);
            StructureGeneratorHelper.generateDynamicFile(workDir,  targetPathElements, fileName, fileName, null);

    }

    public void remove(Path workDir, String serviceInstanceId) {

        try {

            //Remove meta file
            Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.META_FILENAME);
            Files.deleteIfExists(metaFile);

            //Remove secrets file
            Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.SECRETS_FILENAME);
            Files.deleteIfExists(secretsFile);

            //Remove enable deployment file
            Path enableDeploymentFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.ENABLE_DEPLOYMENT_FILENAME);
            Files.deleteIfExists(enableDeploymentFile);


        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.REMOVAL_EXCEPTION);
        }

    }
}