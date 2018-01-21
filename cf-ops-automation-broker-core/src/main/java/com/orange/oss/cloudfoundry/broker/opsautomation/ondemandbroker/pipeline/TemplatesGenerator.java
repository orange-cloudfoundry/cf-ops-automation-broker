package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ijly7474 on 14/12/17.
 */
public class TemplatesGenerator extends StructureGeneratorImpl{

/*
    public TemplatesGenerator(Path workDir, String serviceInstanceId) {
        super(workDir, serviceInstanceId);
    }
*/

    @Override
    public void checkPrerequisites(Path workDir) {
        //Check common pre-requisites
        super.checkPrerequisites(workDir);

        //Check specific pre-requisites
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY);
        if (Files.notExists(templateDir)){
            throw new CassandraProcessorException(CassandraProcessorConstants.TEMPLATE_EXCEPTION);
        }
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
        if (Files.notExists(modelManifestFile)){
            throw new CassandraProcessorException(CassandraProcessorConstants.MANIFEST_FILE_EXCEPTION);
        }
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                CassandraProcessorConstants.MODEL_VARS_FILENAME);
        if (Files.notExists(modelVarsFile)){
            throw new CassandraProcessorException(CassandraProcessorConstants.VARS_FILE_EXCEPTION);
        }
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

        try {

            //Generate service directory
            super.generate(workDir, serviceInstanceId);

            //Generate template directory
            Path deploymentTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            Files.createDirectory(deploymentTemplateDir);

            //Generate deployment dependencies file
            Path targetDeploymentDependenciesFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME);
            Map<String, String> mapDeploymentDependenciesFile = new HashMap<String, String>();
            mapDeploymentDependenciesFile.put(CassandraProcessorConstants.SERVICE_INSTANCE_PATTERN, CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            this.generateFile(CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME,
                    targetDeploymentDependenciesFile,
                    mapDeploymentDependenciesFile);

            //Generate operators file
            Path targetOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.OPERATORS_FILENAME);
            Map<String, String> mapOperatorsFile = new HashMap<String, String>();
            mapOperatorsFile.put(CassandraProcessorConstants.SERVICE_INSTANCE_PATTERN, CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            mapOperatorsFile.put(CassandraProcessorConstants.URL_PATTERN, CassandraProcessorConstants.BROKER_PREFIX + serviceInstanceId);
            this.generateFile(CassandraProcessorConstants.OPERATORS_FILENAME,
                    targetOperatorsFile,
                    mapOperatorsFile);

            //Generate manifest file as symlink
            this.generateFileAsSymbolicLink(CassandraProcessorConstants.MODEL_MANIFEST_FILENAME, CassandraProcessorConstants.MANIFEST_FILENAME_SUFFIX, workDir, serviceInstanceId);

            //Generate vars file as symlink
            this.generateFileAsSymbolicLink(CassandraProcessorConstants.MODEL_VARS_FILENAME, CassandraProcessorConstants.VARS_FILENAME_SUFFIX, workDir, serviceInstanceId);

        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }

    }

    private void generateFile(String sourceFileName, Path targetFile, Map<String, String> findAndReplace){

        try {
            //Read source file content
            List<String> lines = null;
            lines = IOUtils.readLines(getClass().getResourceAsStream(File.separator + CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY + File.separator + sourceFileName), StandardCharsets.UTF_8);

            //Update file content
            List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, findAndReplace);

            //Generate target file
            Files.write(targetFile, resultLines, Charset.forName(StandardCharsets.UTF_8.name()));
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
