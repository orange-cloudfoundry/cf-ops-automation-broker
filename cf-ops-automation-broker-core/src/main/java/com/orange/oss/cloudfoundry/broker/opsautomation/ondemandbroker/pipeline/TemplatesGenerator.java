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

    private String template;
    private String vars;
    private String operators;

    public TemplatesGenerator(){
    }

    public TemplatesGenerator(String rootDeployment, String modelDeployment, String template, String vars, String operators){
        super(rootDeployment,modelDeployment);
        this.template = template;
        this.vars = vars;
        this.operators = operators;
    }

    @Override
    public void checkPrerequisites(Path workDir) {
        //Check common pre-requisites
        super.checkPrerequisites(workDir);

        //Check specific pre-requisite (template directory)
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template);
        if (Files.notExists(templateDir)){
            throw new DeploymentException(DeploymentConstants.TEMPLATE_EXCEPTION);
        }
        //Check specific pre-requisite (operators directory)
        Path operatorsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators);
        if (Files.notExists(operatorsDir)){
            throw new DeploymentException(DeploymentConstants.OPERATORS_EXCEPTION);
        }
        //Check specific pre-requisite (manifest file in model template directory)
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelManifestFile)){
            throw new DeploymentException(DeploymentConstants.MANIFEST_FILE_EXCEPTION);
        }
        //Check specific pre-requisite (vars file in model template directory)
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelVarsFile)){
            throw new DeploymentException(DeploymentConstants.VARS_FILE_EXCEPTION);
        }
        //Check specific pre-requisite (coab operators file in model operators directory)
        Path modelOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators,
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelOperatorsFile)){
            throw new DeploymentException(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);
        }
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

        try {

            //Generate service directory
            super.generate(workDir, serviceInstanceId);

            //Build deploymentInstanceDirectory
            String deploymentInstance = this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId;

            //Generate template directory
            Path deploymentTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    this.rootDeployment,
                    this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId,
                    this.template);
            Files.createDirectory(deploymentTemplateDir);

            //Generate deployment dependencies file
            Map<String, String> mapDeploymentDependenciesFile = new HashMap<String, String>();
            mapDeploymentDependenciesFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, deploymentInstance);
            String[] targetPathElements = new String[] {this.rootDeployment, deploymentInstance};
            String sourceFileName = DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME;
            StructureGeneratorHelper.generateDynamicFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapDeploymentDependenciesFile);

            //Generate manifest file as symlink
            String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
            targetPathElements = new String[] {this.rootDeployment, deploymentInstance, this.template};
            sourceFileName = this.modelDeployment + DeploymentConstants.YML_EXTENSION;
            String targetFileName = deploymentInstance + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);

            //Generate vars file as symlink
            sourceFileName = this.modelDeployment + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
            targetFileName = deploymentInstance + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);

            //Generate coab operators file as symlink
            sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.operators};
            sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, sourceFileName);

            //Generate coab vars file
            Map<String, String> mapCoabVarsFile = new HashMap<String, String>();
            mapCoabVarsFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, deploymentInstance);
            targetPathElements = new String[] {this.rootDeployment, deploymentInstance, this.template};
            sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
            StructureGeneratorHelper.generateDynamicFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapCoabVarsFile);

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
}
