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

    public SecretsGenerator(String rootDeployment, String modelDeployment, String secrets, String meta, String enable){
        super(rootDeployment,modelDeployment);
    }

    public void generate(Path workDir, String serviceInstanceId) {

        try {
            //Generate service directory
            super.generate(workDir, serviceInstanceId);

            //Generate secrets directory
            Path deploymentSecretsDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SECRETS_DIRECTORY);
            Files.createDirectory(deploymentSecretsDir);

            //Generate meta file
            Path metaFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.META_FILENAME);
            Files.write(metaFile, Arrays.asList(CassandraProcessorConstants.META_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //Generate secrets file
            Path secretsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SECRETS_DIRECTORY,
                    CassandraProcessorConstants.SECRETS_FILENAME);
            Files.write(secretsFile, Arrays.asList(CassandraProcessorConstants.SECRETS_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //Generate enable deployment file
            Path enableDeploymentFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.ENABLE_DEPLOYMENT_FILENAME);
            Files.write(enableDeploymentFile, Arrays.asList(CassandraProcessorConstants.ENABLE_DEPLOYMENT_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
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