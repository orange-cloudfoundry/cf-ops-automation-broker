package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TemplatesGenerator extends StructureGeneratorImpl{

    private String template;
    private String vars;
    private String operators;

    public TemplatesGenerator(){
    }

    public TemplatesGenerator(String rootDeployment, String modelDeployment, String template, String vars, String operators, String modelDeploymentShortAlias){
        super(rootDeployment,modelDeployment, modelDeploymentShortAlias);
        this.template = template;
        this.vars = vars;
        this.operators = operators;
    }

    @Override
    public void checkPrerequisites(Path workDir) {
        //Check common pre-requisites
        super.checkPrerequisites(workDir);

        //Check specific pre-requisite (template directory)
        this.checkThatTemplateDirectoryExists(workDir);

        //Check specific pre-requisite (operators directory)
        this.checkThatOperatorsDirectoryExists(workDir);

        //Check specific pre-requisite (manifest file in model template directory)
        this.checkThatModelManifestFileExists(workDir);

        //Check specific pre-requisite (vars file in model template directory)
        this.checkThatModelVarsFileExists(workDir);

        //Check specific pre-requisite (coab operators file in model operators directory)
        this.checkThatModelCoabOperatorsFileExists(workDir);
    }

    @Override
    public void generate(Path workDir, String serviceInstanceId) {

        //Generate service directory
        super.generate(workDir, serviceInstanceId);

        //Build deploymentInstanceDirectory
        String deploymentInstance = this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId;

        //Generate template directory
        this.createTemplateDirectory(workDir, serviceInstanceId);

        //Generate deployment dependencies file
        Map<String, String> mapDeploymentDependenciesFile = new HashMap<>();
        mapDeploymentDependenciesFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, deploymentInstance);
        String[] targetPathElements = new String[] {this.rootDeployment, deploymentInstance};
        String sourceFileName = DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapDeploymentDependenciesFile);

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
        Map<String, String> mapCoabVarsFile = new HashMap<>();
        mapCoabVarsFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, deploymentInstance);
        targetPathElements = new String[] {this.rootDeployment, deploymentInstance, this.template};
        sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapCoabVarsFile);
    }

    private void checkThatTemplateDirectoryExists(Path workDir){
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template);
        if (Files.notExists(templateDir)){
            throw new DeploymentException(DeploymentConstants.TEMPLATE_EXCEPTION);
        }
    }

    private void checkThatOperatorsDirectoryExists(Path workDir){
        Path operatorsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators);
        if (Files.notExists(operatorsDir)){
            throw new DeploymentException(DeploymentConstants.OPERATORS_EXCEPTION);
        }
    }

    private void checkThatModelManifestFileExists(Path workDir){
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelManifestFile)){
            throw new DeploymentException(DeploymentConstants.MANIFEST_FILE_EXCEPTION);
        }
    }

    private void checkThatModelVarsFileExists(Path workDir){
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelVarsFile)){
            throw new DeploymentException(DeploymentConstants.VARS_FILE_EXCEPTION);
        }
    }

    private void checkThatModelCoabOperatorsFileExists(Path workDir){
        Path modelOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators,
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION);
        if (Files.notExists(modelOperatorsFile)){
            throw new DeploymentException(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);
        }
    }

    private String computeDeploymentInstance(String serviceInstanceId){
        return this.modelDeploymentShortAlias + DeploymentConstants.UNDERSCORE + serviceInstanceId;
    }

    private void createTemplateDirectory(Path workDir, String serviceInstanceId){
        try{
            Path deploymentTemplateDir = StructureGeneratorHelper.generatePath(workDir,
                    this.rootDeployment,
                    this.computeDeploymentInstance(serviceInstanceId),
                    this.template);
            Files.createDirectory(deploymentTemplateDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BoshProcessorException(CassandraProcessorConstants.GENERATION_EXCEPTION);
        }
    }

}
