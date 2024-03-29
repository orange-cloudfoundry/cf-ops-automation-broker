package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructureGeneratorHelper {
    private static Logger logger = LoggerFactory.getLogger(StructureGeneratorHelper.class.getName());


    public static Path generatePath(Path rootPath, String... pathElements) {
        Path generatedPath = rootPath;
        if (pathElements != null) {
            for (String s : pathElements)
                generatedPath = generatedPath.resolve(s);
        }
        return generatedPath;
    }

    public static void generateDirectory(Path rootPath, String... pathElements) {
        try {
            //Compute path
            Path directory = generatePath(rootPath, pathElements);

            //Create directory
            if (isMissingResource(directory)) {
                Files.createDirectory(directory);
            }
        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION, e);
        }
    }

    public static void generateSymbolicLink(Path rootPath, String[] sourcePathElements, String[] targetPathElements, String sourceFileName, String targetFileName) {
        try {
            //Compute relative path on directories with relativize method otherwise doesn't work (/!\)
            Path sourceDir = StructureGeneratorHelper.generatePath(rootPath, sourcePathElements);
            Path targetDir = StructureGeneratorHelper.generatePath(rootPath, targetPathElements);
            Path targetToSource = targetDir.relativize(sourceDir);

            //Generate file paths
            Path relativeSourceFile = StructureGeneratorHelper.generatePath(targetToSource, sourceFileName);
            Path absoluteSourceFile = StructureGeneratorHelper.generatePath(sourceDir, sourceFileName);
            Path targetFile = StructureGeneratorHelper.generatePath(targetDir, targetFileName);

            //Create symbolic link
            if (!isMissingResource(absoluteSourceFile) && isMissingResource(targetFile)){ //Test on absolute path is mandatory (/!\)
                Files.createSymbolicLink(targetFile, relativeSourceFile);
            }
        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION, e);
        }
    }


    public static void generateFile(Path rootPath, String[] targetPathElements, String targetFileName, String sourceFileName, Map<String, String> findAndReplace) {
        try {
            //Compute target path
            Path targetDir = StructureGeneratorHelper.generatePath(rootPath, targetPathElements);
            Path targetFile = StructureGeneratorHelper.generatePath(targetDir, targetFileName);

            //Read source file content
            List<String> lines = IOUtils.readLines(StructureGeneratorHelper.class.getResourceAsStream(File.separator + DeploymentConstants.DEPLOYMENT + File.separator + sourceFileName), StandardCharsets.UTF_8);

            //Use map to update file content
            List<String> resultLines;
            if (findAndReplace == null)
                resultLines = lines;
            else
                resultLines = StructureGeneratorHelper.findAndReplace(lines, findAndReplace);

            //Generate target file
            Files.write(targetFile, resultLines, Charset.forName(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.GENERATION_EXCEPTION, e);
        }
    }

    public static void removeFile(Path rootPath, String[] pathElements, String fileName) {
        try {
            //Compute path
            Path dir = StructureGeneratorHelper.generatePath(rootPath, pathElements);
            Path file = StructureGeneratorHelper.generatePath(dir, fileName);

            //Delete file
            Files.deleteIfExists(file);

        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.REMOVAL_EXCEPTION, e);
        }
    }

    public static void removeRecursivelyDirectory(Path rootPath, String[] pathElements) {
        try {
            //Compute path
            Path dir = StructureGeneratorHelper.generatePath(rootPath, pathElements);

            if (! Files.exists(dir)) {
                logger.info("Asked to delete empty dir at {} Returning successfully", dir);
                return;
            }

            //Delete directory
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.REMOVAL_EXCEPTION, e);
        }
    }

    public static List<String> findAndReplace(List<String> lines, Map<String, String> map) {
        List<String> resultLines = new ArrayList<>();
        for (String s : lines) {
            String resultLine = null;
            for (Map.Entry mapentry : map.entrySet()) {
                resultLine = s.replaceFirst((String) mapentry.getKey(), (String) mapentry.getValue());
                if (!s.equals(resultLine)) {
                    break;
                }
            }
            resultLines.add(resultLine);
        }
        return resultLines;
    }

    public static boolean isMissingResource(Path path) {
        return Files.notExists(path, LinkOption.NOFOLLOW_LINKS);
    }

    //https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
    public static List<String> listFilesPaths_old(Path path, String glob) {
        List<String> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(path, glob)) {
            for (Path entry : stream) {
                paths.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.SEARCH_EXCEPTION, e);
        }
        return paths;
    }

    public static List<String> listFilesPaths(Path path, String glob) {
        List<String> paths = new ArrayList<>();

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                glob);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path,
                                                 BasicFileAttributes attrs) {
                    if (pathMatcher.matches(path)) {
                        paths.add(path.getParent().getFileName().toString() + File.separator + path.getFileName().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new DeploymentException(DeploymentConstants.SEARCH_EXCEPTION, e);
        }
        return paths;
    }

    public static String getDirectory(String path) {
        String[] parts = path.split(File.separator);
        return parts[0];
    }

    public static String getFile(String path) {
        String[] parts = path.split(File.separator);
        return parts[1];
    }

}