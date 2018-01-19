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

    public void setWorkDir(Path workDir){
        this.workDir = workDir;
    }

    public void setServiceInstanceId(String serviceInstanceId){
        this.serviceInstanceId = serviceInstanceId;
    }

    public StructureGeneratorImpl(Path workDir, String serviceInstanceId){
        this.workDir = workDir;
        this.serviceInstanceId = serviceInstanceId;
    }

    public void checkPrerequisites() {
        Path rootDeploymentDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY);
        if (Files.notExists(rootDeploymentDir)){
            throw new CassandraProcessorException(CassandraProcessorConstants.ROOT_DEPLOYMENT_EXCEPTION);
        }
        Path modelDeploymentDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY);
        if (Files.notExists(modelDeploymentDir)){
            throw new CassandraProcessorException(CassandraProcessorConstants.MODEL_DEPLOYMENT_EXCEPTION);
        }
    }

    public void generate() {
        try {
            //Generate service directory
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            Files.createDirectory(serviceInstanceDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

}
