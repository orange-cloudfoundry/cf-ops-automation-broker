package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ijly7474 on 14/12/17.
 */
public class TemplatesGenerator extends StructureGeneratorImpl{

    public TemplatesGenerator(Path workDir, String serviceInstanceId) {
        super(workDir, serviceInstanceId);
    }

    @Override
    public void generate() {

        try {

            //Generate service directory
            super.generate();

            //Generate template directory
            Path pathDeploymentTemplate = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            Files.createDirectory(pathDeploymentTemplate);

            //Generate deployment dependencies files
            this.generateDeploymentDependenciesFile();


        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }

    }

    private void generateDeploymentDependenciesFile(){

        try {
            List<String> lines = null;
            lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME).toURI()));
            Path secretsPath = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + File.separator + CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME);
            Files.write(secretsPath, lines, Charset.forName(StandardCharsets.UTF_8.name()));

        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

}
