package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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

        //Generate template directory
        this.generateTemplateDirectory(workDir, serviceInstanceId);

        //Generate deployment dependencies file
        this.generateDeploymentDependenciesFile(workDir, serviceInstanceId);

        //Generate manifest file as symlink
        this.generateManifestFileSymLink(workDir, serviceInstanceId);

        //Generate coab vars file as symlink
        this.generateCoabOperatorsFileSymLink(workDir, serviceInstanceId);

        //Generate vars file as symlink
        this.generateVarsFileSymLink(workDir, serviceInstanceId);

        //Generate coab vars file
        this.generateCoabVarsFile(workDir, serviceInstanceId);

    }

    private void checkThatTemplateDirectoryExists(Path workDir){
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template);
        if (StructureGeneratorHelper.isMissingResource(templateDir)){
            throw new DeploymentException(DeploymentConstants.TEMPLATE_EXCEPTION);
        }
    }

    private void checkThatOperatorsDirectoryExists(Path workDir){
        Path operatorsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators);
        if (StructureGeneratorHelper.isMissingResource(operatorsDir)){
            throw new DeploymentException(DeploymentConstants.OPERATORS_EXCEPTION);
        }
    }

    private void checkThatModelManifestFileExists(Path workDir){
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(modelManifestFile)){
            throw new DeploymentException(DeploymentConstants.MANIFEST_FILE_EXCEPTION);
        }
    }

    private void checkThatModelVarsFileExists(Path workDir){
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(modelVarsFile)){
            throw new DeploymentException(DeploymentConstants.VARS_FILE_EXCEPTION);
        }
    }

    private void checkThatModelCoabOperatorsFileExists(Path workDir){
        Path modelOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators,
                DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION);
        if (StructureGeneratorHelper.isMissingResource(modelOperatorsFile)){
            throw new DeploymentException(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);
        }
    }

    private void generateTemplateDirectory(Path workDir, String serviceInstanceId){
        StructureGeneratorHelper.generateDirectory(workDir,
                this.rootDeployment,
                this.computeDeploymentInstance(serviceInstanceId),
                this.template);
    }

    private void generateDeploymentDependenciesFile(Path workDir, String serviceInstanceId){
        Map<String, String> mapDeploymentDependenciesFile = new HashMap<>();
        mapDeploymentDependenciesFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, this.computeDeploymentInstance(serviceInstanceId));
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId)};
        String sourceFileName = DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapDeploymentDependenciesFile);
    }

    private void generateManifestFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.YML_EXTENSION;
        String targetFileName = this.computeDeploymentInstance(serviceInstanceId) + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
    }

    private void generateVarsFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        String targetFileName = this.computeDeploymentInstance(serviceInstanceId) + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
    }

    private void generateCoabOperatorsFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.operators};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.template};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, sourceFileName);
    }

/*
    public void generateOperatorsFileSymLinks(Path workDir, String serviceInstanceId) {
        Path path = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment, this.template);
        List<String> modelOperators = StructureGeneratorHelper.listFilesPaths(path, "*" + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.template};
        for (String s: modelOperators) {
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, s, s);
        }
    }
*/

    private void generateCoabVarsFile(Path workDir, String serviceInstanceId){
        Map<String, String> mapCoabVarsFile = new HashMap<>();
        mapCoabVarsFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, this.computeDeploymentInstance(serviceInstanceId));
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentInstance(serviceInstanceId), this.template};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapCoabVarsFile);
    }

}
