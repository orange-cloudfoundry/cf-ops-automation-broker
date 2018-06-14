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
        //this.checkThatOperatorsDirectoryExists(workDir); => useless

        //Check specific pre-requisite (manifest file in model template directory)
        this.checkThatModelManifestFileExists(workDir);

        //Check specific pre-requisite (vars file in model template directory)
        //this.checkThatModelVarsFileExists(workDir); => useless

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

        //Generate vars file as symlink
        this.generateVarsFileSymLink(workDir, serviceInstanceId);

        //Generate coab specific ops file as symlink
        this.generateCoabOperatorsFileSymLink(workDir, serviceInstanceId);

        //Generate all ops files as symlink
        this.generateOperatorsFileSymLinks(workDir, serviceInstanceId);

        //Generate coab vars file
        this.generateCoabVarsFile(workDir, serviceInstanceId);

    }

    protected void checkThatTemplateDirectoryExists(Path workDir){
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template);
        if (StructureGeneratorHelper.isMissingResource(templateDir)){
            throw new DeploymentException(DeploymentConstants.TEMPLATE_EXCEPTION);
        }
    }

    protected void checkThatOperatorsDirectoryExists(Path workDir){
        Path operatorsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators);
        if (StructureGeneratorHelper.isMissingResource(operatorsDir)){
            throw new DeploymentException(DeploymentConstants.OPERATORS_EXCEPTION);
        }
    }

    protected void checkThatModelManifestFileExists(Path workDir){
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.YML_EXTENSION);
        Path modelManifestTplFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        if (StructureGeneratorHelper.isMissingResource(modelManifestFile) && StructureGeneratorHelper.isMissingResource(modelManifestTplFile)){
            throw new DeploymentException(DeploymentConstants.MANIFEST_FILE_EXCEPTION);
        }
    }

    protected void checkThatModelVarsFileExists(Path workDir){
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION);
        Path modelVarsTplFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        if (StructureGeneratorHelper.isMissingResource(modelVarsFile) && StructureGeneratorHelper.isMissingResource(modelVarsTplFile)){
            throw new DeploymentException(DeploymentConstants.VARS_FILE_EXCEPTION);
        }
    }

    protected void checkThatModelCoabOperatorsFileExists(Path workDir){
        //Temporary situation
        Path modelOperatorsFileinSpecificPath = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators,
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        //Target situation
        Path modelOperatorsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.template,
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        if (StructureGeneratorHelper.isMissingResource(modelOperatorsFileinSpecificPath) && StructureGeneratorHelper.isMissingResource(modelOperatorsFile)){
            throw new DeploymentException(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);
        }
    }

    protected void generateTemplateDirectory(Path workDir, String serviceInstanceId){
        StructureGeneratorHelper.generateDirectory(workDir,
                this.rootDeployment,
                this.computeDeploymentName(serviceInstanceId),
                this.template);
    }

    protected void generateDeploymentDependenciesFile(Path workDir, String serviceInstanceId){
        Map<String, String> mapDeploymentDependenciesFile = new HashMap<>();
        mapDeploymentDependenciesFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, this.computeDeploymentName(serviceInstanceId));
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String sourceFileName = DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapDeploymentDependenciesFile);
    }

    protected void generateManifestFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.YML_EXTENSION;
        String targetFileName = this.computeDeploymentName(serviceInstanceId) + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
    }

    protected void generateManifestFileSymLinkNew(Path workDir, String serviceInstanceId){
        List<String> manifestFileList = this.searchForManifestFiles(workDir);
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.YML_EXTENSION;
        for (String manifestFile: manifestFileList) { //can have -tpl or not
            String targetFileName = manifestFile.replaceFirst(this.modelDeployment, this.computeDeploymentName(serviceInstanceId));
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
        }
    }

    protected void generateVarsFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION;
        String targetFileName = this.computeDeploymentName(serviceInstanceId) + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
    }

    protected void generateVarsFileSymLinkNew(Path workDir, String serviceInstanceId){
        List<String> varsFileList = this.searchForVarsFiles(workDir);
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION;
        for (String varsFile: varsFileList) { //can have -tpl or not
            String targetFileName = varsFile.replaceFirst(this.modelDeployment, this.computeDeploymentName(serviceInstanceId));
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, targetFileName);
        }
    }

    protected void generateCoabOperatorsFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.operators};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, sourceFileName);
    }

    protected void generateOperatorsFileSymLinks(Path workDir, String serviceInstanceId) {
        List<String> operatorsFileList = this.searchForOperatorsFiles(workDir);
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.template};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        for (String operatorFile: operatorsFileList) {
            StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, operatorFile, operatorFile);
        }
    }

    protected void generateCoabVarsFile(Path workDir, String serviceInstanceId){
        Map<String, String> mapCoabVarsFile = new HashMap<>();
        mapCoabVarsFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, this.computeDeploymentName(serviceInstanceId));
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), this.template};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.vars + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapCoabVarsFile);
    }

    protected List<String> searchForManifestFiles(Path workDir){
        Path path = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment, this.template);
        String glob = "{" + this.modelDeployment + DeploymentConstants.YML_EXTENSION + "," + this.modelDeployment + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX + "}"; // "{model.yml,model-tpl.yml}"
        List<String> manifestFileList = StructureGeneratorHelper.listFilesPaths(path, glob);
        return manifestFileList;
    }

    protected List<String> searchForVarsFiles(Path workDir){
        Path path = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment, this.template);
        String glob = "{" + this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION + "," + this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX + "}"; // "{model-vars.yml,model-vars-tpl.yml}"
        List<String> varsFileList = StructureGeneratorHelper.listFilesPaths(path, glob);
        return varsFileList;
    }

    protected List<String> searchForOperatorsFiles(Path workDir){ //Search across sub directory
        Path path = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment, this.template);
        String glob = "*" + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX; // "*-operators.yml"
        List<String> operatorsFileList = StructureGeneratorHelper.listFilesPaths(path, glob);
        return operatorsFileList;
    }



}
