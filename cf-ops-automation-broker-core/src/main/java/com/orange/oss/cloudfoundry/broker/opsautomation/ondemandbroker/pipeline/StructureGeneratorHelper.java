package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ijly7474 on 18/12/17.
 */
public class StructureGeneratorHelper {

    public static Path generatePath(Path rootPath, String... pathElements){
        Path generatedPath = rootPath;
        if (pathElements != null){
            for ( String s : pathElements )
                generatedPath = generatedPath.resolve(s);
        }
        return generatedPath;
    }

    public static void generateDirectory(Path rootPath, String... pathElements) {
        try{
            //Compute path
            Path directory = generatePath(rootPath, pathElements);

            //Create directory
            Files.createDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION);
        }
    }

    public static void generateSymbolicLink(Path rootPath, String[] sourcePathElements, String[] targetPathElements, String sourceFileName, String targetFileName) {
        try{
            //Compute relative path on directories with relativize method otherwise doesn't work
            Path sourceDir = StructureGeneratorHelper.generatePath(rootPath, sourcePathElements);
            Path targetDir = StructureGeneratorHelper.generatePath(rootPath, targetPathElements);
            Path targetToSource = targetDir.relativize(sourceDir);

            //Generate file paths
            Path relativeSourceFile = StructureGeneratorHelper.generatePath(targetToSource, sourceFileName);
            Path targetFile = StructureGeneratorHelper.generatePath(targetDir, targetFileName);

            //Create symbolic link
            Files.createSymbolicLink(targetFile, relativeSourceFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION);
        }
    }


    public static void generateFile(Path rootPath, String[] targetPathElements, String targetFileName, String sourceFileName, Map<String, String> findAndReplace) {
        try{
            //Compute target path
            Path targetDir = StructureGeneratorHelper.generatePath(rootPath, targetPathElements);
            Path targetFile = StructureGeneratorHelper.generatePath(targetDir, targetFileName);

            //Read source file content
            List<String> lines = null;
            lines = IOUtils.readLines(StructureGeneratorHelper.class.getResourceAsStream(File.separator + DeploymentConstants.DEPLOYMENT + File.separator + sourceFileName), StandardCharsets.UTF_8);

            //Use map to update file content
            List<String> resultLines;
            if (findAndReplace == null)
                resultLines = lines;
            else
                resultLines = StructureGeneratorHelper.findAndReplace(lines, findAndReplace);

            //Generate target file
            Files.write(targetFile, resultLines, Charset.forName(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION);
        }
    }

    public static void removeFile(Path rootPath, String[] pathElements, String fileName) {
        try{
            //Compute path
            Path dir = StructureGeneratorHelper.generatePath(rootPath, pathElements);
            Path file = StructureGeneratorHelper.generatePath(dir, fileName);

            //Delete file
            Files.deleteIfExists(file);

        } catch (IOException e) {
            e.printStackTrace();
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION);
        }
    }

    public static List<String> findAndReplace(List<String> lines, Map<String, String> map){
        List<String> resultLines = new ArrayList<String>();
        for (String s : lines){
            String resultLine = null;
            for (Map.Entry mapentry : map.entrySet()) {
                resultLine = s.replaceFirst((String)mapentry.getKey(), (String)mapentry.getValue());
                if (!s.equals(resultLine)){
                    break;
                }
            }
            resultLines.add(resultLine);
        }
        return resultLines;
    }

    public static boolean isMissingResource(Path path){
        return Files.notExists(path);
    }
}
