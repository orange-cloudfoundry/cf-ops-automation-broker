package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ijly7474 on 15/12/17.
 */
public class StructureGeneratorImpl implements StructureGenerator {

    protected Path workDir;
    protected String serviceInstanceId;


    public StructureGeneratorImpl(Path workDir, String serviceInstanceId){
        this.workDir = workDir;
        this.serviceInstanceId = serviceInstanceId;
    }

    public void checkPrerequisites() {
        Path rootDeployment = Paths.get(String.valueOf(workDir)+ File.separator  + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
        if (Files.notExists(rootDeployment)){
            throw new CassandraProcessorException(CassandraProcessorConstants.ROOT_DEPLOYMENT_EXCEPTION);
        }
        Path modelDeployment = Paths.get(String.valueOf(workDir)+ File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
        if (Files.notExists(modelDeployment)){
            throw new CassandraProcessorException(CassandraProcessorConstants.MODEL_DEPLOYMENT_EXCEPTION);
        }
    }

    public void generate() {
        try {
            //Generate service directory
            Path pathDeployment = Paths.get(String.valueOf(workDir) + File.separator + CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            Files.createDirectory(pathDeployment);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

}
