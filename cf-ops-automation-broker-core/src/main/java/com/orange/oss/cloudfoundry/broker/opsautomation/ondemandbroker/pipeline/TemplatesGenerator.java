package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.apache.commons.collections.map.HashedMap;

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

    public TemplatesGenerator(Path workDir, String serviceInstanceId) {
        super(workDir, serviceInstanceId);
    }

    @Override
    public void checkPrerequisites() {
        //Check common pre-requisites
        super.checkPrerequisites();

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
    public void generate() {

        try {

            //Generate service directory
            super.generate();

            //Generate template directory
            Path deploymentTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY);
            Files.createDirectory(deploymentTemplateDir);

            //Generate deployment dependencies file
            this.generateDeploymentDependenciesFile();

            //Generate operators file
            this.generateOperatorsFile();

            //Generate manifest file as symlink
            this.generateManifestSymbolicLink();


        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }

    }

    private void generateDeploymentDependenciesFile(){

        try {
            List<String> lines = null;
            //TODO : Change path building
            lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME).toURI()));

            //Generate target path
            Path targetDeploymentDependenciesFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.DEPLOYMENT_DEPENDENCIES_FILENAME);
            //Update file content
            Map<String, String> map = new HashMap<String, String>();
            map.put(CassandraProcessorConstants.SERVICE_INSTANCE_PATTERN, CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, map);
            //Generate target file
            Files.write(targetDeploymentDependenciesFile, resultLines, Charset.forName(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

    private void generateOperatorsFile(){

        try {
            List<String> lines = null;
            //TODO : Change path building
            lines = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY + File.separator + CassandraProcessorConstants.OPERATORS_FILENAME).toURI()));
            Path targetOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.OPERATORS_FILENAME);
            //Update file content
            Map<String, String> map = new HashMap<String, String>();
            map.put(CassandraProcessorConstants.SERVICE_INSTANCE_PATTERN, CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId);
            map.put(CassandraProcessorConstants.URL_PATTERN, CassandraProcessorConstants.BROKER_PREFIX + serviceInstanceId);
            List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, map);
            Files.write(targetOperatorsFile, resultLines, Charset.forName(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

    private void generateManifestSymbolicLink(){

        try {
            Path targetPath = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.MODEL_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.MODEL_MANIFEST_FILENAME);
            Path linkPath = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.TEMPLATE_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.MANIFEST_FILENAME_SUFFIX);
            targetPath = targetPath.relativize(linkPath);
            Files.createSymbolicLink(linkPath, targetPath);

        } catch (IOException e) {
            e.printStackTrace();
            throw new CassandraProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }



}
