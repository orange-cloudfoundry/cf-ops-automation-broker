package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplatesGenerator extends StructureGeneratorImpl{

    private static Logger logger = LoggerFactory.getLogger(TemplatesGenerator.class.getName());

    private String operators;
    private VarsFilesYmlFormatter formatter;

    public TemplatesGenerator(String rootDeployment, String modelDeployment, String operators, String modelDeploymentShortAlias, VarsFilesYmlFormatter formatter){
        super(rootDeployment,modelDeployment, modelDeploymentShortAlias);
        this.operators = operators;
        this.formatter = formatter;
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
    public void generate(Path workDir, String serviceInstanceId, CoabVarsFileDto coabVarsFileDto) {

        //Generate service directory
        super.generate(workDir, serviceInstanceId, coabVarsFileDto);

        //Generate template directory
        this.generateTemplateDirectory(workDir, serviceInstanceId);

        //Generate deployment dependencies file
        this.generateDeploymentDependenciesFile(workDir, serviceInstanceId);

        //Generate coab vars file
        this.generateCoabVarsFile(workDir, serviceInstanceId, coabVarsFileDto);

        //Generate coab specific ops file as symlink (old deployment model for backward compatibility)
        this.generateCoabOperatorsFileSymLink(workDir, serviceInstanceId);

        //Generate manifest, vars et operators symlinks
        this.generateAllSymLinks(workDir, serviceInstanceId);

    }

    protected void checkThatTemplateDirectoryExists(Path workDir){
        Path templateDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                DeploymentConstants.TEMPLATE);
        if (StructureGeneratorHelper.isMissingResource(templateDir)){
            throw new DeploymentException(DeploymentConstants.TEMPLATE_EXCEPTION + templateDir);
        }
    }

    protected void checkThatOperatorsDirectoryExists(Path workDir){
        Path operatorsDir = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                this.operators);
        if (StructureGeneratorHelper.isMissingResource(operatorsDir)){
            throw new DeploymentException(DeploymentConstants.OPERATORS_EXCEPTION + operatorsDir);
        }
    }

    protected void checkThatModelManifestFileExists(Path workDir){
        Path modelManifestFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                DeploymentConstants.TEMPLATE,
                this.modelDeployment + DeploymentConstants.YML_EXTENSION);
        Path modelManifestTplFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                DeploymentConstants.TEMPLATE,
                this.modelDeployment + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
        if (StructureGeneratorHelper.isMissingResource(modelManifestFile) && StructureGeneratorHelper.isMissingResource(modelManifestTplFile)){
            throw new DeploymentException(DeploymentConstants.MANIFEST_FILE_EXCEPTION);
        }
    }

    protected void checkThatModelVarsFileExists(Path workDir){
        Path modelVarsFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                DeploymentConstants.TEMPLATE,
                this.modelDeployment + DeploymentConstants.COA_VARS_FILE + DeploymentConstants.YML_EXTENSION);
        Path modelVarsTplFile = StructureGeneratorHelper.generatePath(workDir,
                this.rootDeployment,
                this.modelDeployment,
                DeploymentConstants.TEMPLATE,
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
                DeploymentConstants.TEMPLATE,
                DeploymentConstants.COAB + DeploymentConstants.COA_OPERATORS_FILE_SUFFIX);
        if (StructureGeneratorHelper.isMissingResource(modelOperatorsFileinSpecificPath) && StructureGeneratorHelper.isMissingResource(modelOperatorsFile)){
            throw new DeploymentException(DeploymentConstants.COAB_OPERATORS_FILE_EXCEPTION);
        }
    }

    protected void generateTemplateDirectory(Path workDir, String serviceInstanceId){
        StructureGeneratorHelper.generateDirectory(workDir,
                this.rootDeployment,
                this.computeDeploymentName(serviceInstanceId),
                DeploymentConstants.TEMPLATE);
    }

    protected void generateDeploymentDependenciesFile(Path workDir, String serviceInstanceId){
        Map<String, String> mapDeploymentDependenciesFile = new HashMap<>();
        mapDeploymentDependenciesFile.put(DeploymentConstants.DEPLOYMENT_NAME_PATTERN, this.computeDeploymentName(serviceInstanceId));
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId)};
        String sourceFileName = DeploymentConstants.DEPLOYMENT_DEPENDENCIES_FILENAME;
        StructureGeneratorHelper.generateFile(workDir, targetPathElements, sourceFileName, sourceFileName, mapDeploymentDependenciesFile);
    }

    protected void generateCoabVarsFile(Path workDir, String serviceInstanceId, CoabVarsFileDto coabVarsFileDto){
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), DeploymentConstants.TEMPLATE};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + DeploymentConstants.VARS + DeploymentConstants.YML_EXTENSION;

        //Compute target path
        Path targetDir = StructureGeneratorHelper.generatePath(workDir, targetPathElements);
        Path targetFile = StructureGeneratorHelper.generatePath(targetDir, sourceFileName);

        try {
            //Format as yml
            String varsFileYmlContent = formatter.formatAsYml(coabVarsFileDto);

            //write target file
            Files.write(targetFile, varsFileYmlContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            String msg = "Unable to generate vars file in " + targetFile + " caught: " + e;
            logger.error(msg, e);
            throw new DeploymentException(msg);
        }
    }

    protected void generateCoabOperatorsFileSymLink(Path workDir, String serviceInstanceId){
        String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, this.operators};
        String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), DeploymentConstants.TEMPLATE};
        String sourceFileName = DeploymentConstants.COAB + DeploymentConstants.HYPHEN + this.operators + DeploymentConstants.YML_EXTENSION;
        StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, sourceFileName, sourceFileName);
    }

    protected void generateAllSymLinks(Path workDir, String serviceInstanceId){
        List<String> pathList = this.searchForAllFiles(workDir);
        for (String path: pathList) {
            if (this.isIaasType(path)){
                StructureGeneratorHelper.generateDirectory(workDir,
                                this.rootDeployment,
                                                this.computeDeploymentName(serviceInstanceId),
                        DeploymentConstants.TEMPLATE,
                                                StructureGeneratorHelper.getDirectory(path)
                        );
                String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, DeploymentConstants.TEMPLATE, StructureGeneratorHelper.getDirectory(path)};
                String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), DeploymentConstants.TEMPLATE, StructureGeneratorHelper.getDirectory(path)};
                StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, StructureGeneratorHelper.getFile(path), StructureGeneratorHelper.getFile(path));
            } else {
                String[] sourcePathElements = new String[] {this.rootDeployment, this.modelDeployment, DeploymentConstants.TEMPLATE};
                String[] targetPathElements = new String[] {this.rootDeployment, this.computeDeploymentName(serviceInstanceId), DeploymentConstants.TEMPLATE};
                String targetFileName = isManifest(StructureGeneratorHelper.getFile(path)) ? StructureGeneratorHelper.getFile(path).replaceFirst(this.modelDeployment, this.computeDeploymentName(serviceInstanceId)) : StructureGeneratorHelper.getFile(path);
                StructureGeneratorHelper.generateSymbolicLink(workDir, sourcePathElements, targetPathElements, StructureGeneratorHelper.getFile(path), targetFileName);
            }
        }
    }

    protected List<String> searchForAllFiles(Path workDir){
        Path path = StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, this.modelDeployment, DeploymentConstants.TEMPLATE);
        return StructureGeneratorHelper.listFilesPaths(path, "glob:**/*");
    }

    protected boolean isManifest(String fileName){
        return fileName.contentEquals(this.modelDeployment + DeploymentConstants.YML_EXTENSION) ||
                fileName.contentEquals(this.modelDeployment + DeploymentConstants.COA_TEMPLATE_FILE_SUFFIX);
    }

    protected boolean isIaasType(String path){
        return ! StructureGeneratorHelper.getDirectory(path).contentEquals(DeploymentConstants.TEMPLATE);
    }

}
