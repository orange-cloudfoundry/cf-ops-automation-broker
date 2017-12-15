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

    public SecretsGenerator(Path workDir, String serviceInstanceId) {
        super(workDir, serviceInstanceId);
    }

    public void generate() {

        try {
            //Generate secrets directory
            super.generate();

            //Generate secrets directory
            Path pathDeploymentSecrets = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.SECRETS_DIRECTORY);
            Files.createDirectory(pathDeploymentSecrets);

            //Generate meta file
            Path metaPath = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.SECRETS_DIRECTORY + File.separator+ CassandraProcessorConstants.META_FILENAME);
            Files.write(metaPath, Arrays.asList(CassandraProcessorConstants.META_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //Generate secrets file
            Path secretsPath = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.SECRETS_DIRECTORY + File.separator + CassandraProcessorConstants.SECRETS_FILENAME);
            Files.write(secretsPath, Arrays.asList(CassandraProcessorConstants.SECRETS_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

            //Generate enable deployment file
            Path enableDeploymentPath = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.ENABLE_DEPLOYMENT_FILENAME);
            Files.write(enableDeploymentPath, Arrays.asList(CassandraProcessorConstants.ENABLE_DEPLOYMENT_CONTENT), Charset.forName(StandardCharsets.UTF_8.name()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
