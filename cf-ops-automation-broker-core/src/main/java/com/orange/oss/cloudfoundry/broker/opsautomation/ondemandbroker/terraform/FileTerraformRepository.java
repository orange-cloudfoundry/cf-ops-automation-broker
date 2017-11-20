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
import java.util.Optional;
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
        Optional<ImmutableTerraformModule> module = findAllAsStream()
                .filter(m -> propertyValue.equals(m.getProperties().get(propertyName)))
                .findFirst();

        return module.orElse(null);
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
        File file = directory.resolve(filePrefix + name + ".tf").toFile();
        if (!directory.equals(file.getParentFile().toPath())) {
            logger.error("unexpectedly trying to save outside of expected dir. Getting FS injection attempt? Module name=" + name);
            throw new RuntimeException("invalid module name");
        }
        return file;
    }


    List<ImmutableTerraformModule> findAll(){
        Stream<ImmutableTerraformModule> modules = findAllAsStream();
        return modules
                .collect(Collectors.toList());
    }

    protected Stream<ImmutableTerraformModule> findAllAsStream() {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**" + filePrefix +"*.tf");

        Stream<Path> list;
        try {
            list = Files.list(directory);
        } catch (IOException e) {
            logger.error("unable to list module from " + directory, e);
            throw new RuntimeException("unable to list modules");
        }

        return list
                .filter(matcher::matches)
                .map(path -> {
                    try (Reader reader = new FileReader(path.toFile())) {
                        return gson.fromJson(reader, ImmutableTerraformModule.class);
                    } catch (IOException e) {
                        logger.error("unable to load module from " + path, e);
                        throw new RuntimeException("unable to load modules");
                    }

                });
    }
}
