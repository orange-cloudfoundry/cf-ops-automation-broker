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

    protected String rootDeployment;
    protected String modelDeployment;

    public StructureGeneratorImpl(){
    }

    public StructureGeneratorImpl(String rootDeployment, String modelDeployment){
        this.rootDeployment = rootDeployment;
        this.modelDeployment = modelDeployment;
    }

    public void checkPrerequisites(Path workDir) {
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

    public void generate(Path workDir, String serviceInstanceId) {
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

    private void generateFileAsSymbolicLink(String modelFileName, String modelFileNameSuffix, Path workDir, String serviceInstanceId) {
        try {

            //Compute relative path on directories with relativize method otherwise doesn't work
            Path modelTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);

            Path serviceTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            Path serviceToModel = serviceTemplateDir.relativize(modelTemplateDir);

            //Generate file paths
            Path relativeModelManifestFile = StructureGeneratorHelper.generatePath(serviceToModel,
                    modelFileName);

            Path serviceManifestFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + modelFileNameSuffix);

            //Generate symbolic link
            Files.createSymbolicLink(serviceManifestFile, relativeModelManifestFile);

        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }



}
