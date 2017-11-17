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
import java.util.NoSuchElementException;
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
        if (filePrefix == null) {
            filePrefix = "";
        }
        this.filePrefix = filePrefix;
        gson = new GsonBuilder().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
    }

    @Override
    public TerraformModule getByModuleId(String moduleId) {
        File file = buildFileForModule(moduleId);
        if (! file.exists()) {
            return null;
        }

        try (FileReader writer = new FileReader(file)) {
            ImmutableTerraformModule immutableTerraformModule = gson.fromJson(writer, ImmutableTerraformModule.class);
            return ImmutableTerraformModule.builder()
                    .from(immutableTerraformModule)
                    .id(moduleId)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("unable to load tf module " + moduleId, e);
        }
    }

    @Override
    public TerraformModule save(TerraformModule module) {
        String moduleId = module.getId();
        File file = buildFileForModule(moduleId);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(module, writer);
        } catch (IOException e) {
            logger.error("unable to save module " + module  + " into " + file, e);
            throw new RuntimeException("unable to save tf module ", e);
        }
        return module;
    }

    public List<ImmutableTerraformModule> findAll() throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**" + filePrefix +"*.tf");

        Stream<Path> list = Files.list(directory);

        return list
                .filter(matcher::matches)
                .map(path -> {
                    try (Reader reader = new FileReader(path.toFile())) {
                        ImmutableTerraformModule module = gson.fromJson(reader, ImmutableTerraformModule.class);
                        return ImmutableTerraformModule.builder()
                                .from(module)
                                .id(module.getModuleName())
                                .build();
                    } catch (IOException e) {
                        logger.error("unable to load module from " + path, e);
                        throw new RuntimeException("unable to load modules");
                    }

                })
                .collect(Collectors.toList());
    }


    public File buildFileForModule(String id) {
        return directory.resolve(filePrefix + id + ".tf").toFile();
    }

    @Override
    public TerraformModule getByModuleName(String moduleName) {
        return getByModuleId(moduleName);
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
        File moduleFile = buildFileForModule(module.getId());
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
}
