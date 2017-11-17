package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class FileTerraformRepository implements TerraformRepository {
    private final Gson gson;
    private Path directory;
    private String filePrefix;
    private static Logger logger = LoggerFactory.getLogger(FileTerraformRepository.class.getName());

    public FileTerraformRepository(Path directory, String filePrefix) {
        this.directory = directory;
        if (filePrefix == null || filePrefix.isEmpty()) {
            throw new IllegalArgumentException("expected non empty prefix. This is required to support ignoring some unrelated files in the directory that don't match the prefix.");
        }
        this.filePrefix = filePrefix;
        gson = new GsonBuilder().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
    }

    @Override
    public TerraformModule save(TerraformModule module) {
        String moduleName = module.getModuleName();
        File file = buildFileForModule(moduleName);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(module, writer);
        } catch (IOException e) {
            logger.error("unable to save module " + module  + " into " + file, e);
            throw new RuntimeException("unable to save tf module named: " + moduleName, e);
        }
        return module;
    }

    @Override
    public TerraformModule getByModuleName(String moduleName) {
        File file = buildFileForModule(moduleName);
        if (! file.exists()) {
            return null;
        }

        try (FileReader writer = new FileReader(file)) {
            return gson.fromJson(writer, ImmutableTerraformModule.class);
        } catch (IOException e) {
            throw new RuntimeException("unable to load tf module " + moduleName, e);
        }
    }

    @Override
    public TerraformModule getByModuleProperty(String propertyName, String propertyValue) {
        try {
            List<ImmutableTerraformModule> modules = findAll();
            for (ImmutableTerraformModule module : modules) {
                if (propertyValue.equals(module.getProperties().get(propertyName))) {
                    return module;
                }
            }
        } catch (IOException e) {
            logger.error("Unable to load module at: " + directory, e);

            throw new RuntimeException("unable to load modules", e);
        }
        return null;
    }

    @Override
    public void delete(TerraformModule module) {
        File moduleFile = buildFileForModule(module.getModuleName());
        if (moduleFile.exists()) {
            Path path = moduleFile.toPath();
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.error("Unable to delete file at: " + path, e);
                throw new RuntimeException("unable to delete module", e);
            }
        }
    }

    File buildFileForModule(String name) {
        return directory.resolve(filePrefix + name + ".tf").toFile();
    }


    List<ImmutableTerraformModule> findAll() throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**" + filePrefix +"*.tf");

        Stream<Path> list = Files.list(directory);

        return list
                .filter(matcher::matches)
                .map(path -> {
                    try (Reader reader = new FileReader(path.toFile())) {
                        ImmutableTerraformModule module = gson.fromJson(reader, ImmutableTerraformModule.class);
                        return ImmutableTerraformModule.builder()
                                .from(module)
                                .build();
                    } catch (IOException e) {
                        logger.error("unable to load module from " + path, e);
                        throw new RuntimeException("unable to load modules");
                    }

                })
                .collect(Collectors.toList());
    }
}
