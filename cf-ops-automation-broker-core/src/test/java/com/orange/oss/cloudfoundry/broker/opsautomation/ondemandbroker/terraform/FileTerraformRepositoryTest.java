package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class FileTerraformRepositoryTest {

    Path tempDirectory;
    FileTerraformRepository repository;

    @Before
    public void setUpTempDir() throws IOException {
        tempDirectory = Files.createTempDirectory(FileTerraformRepositoryTest.class.getSimpleName());
        repository = new FileTerraformRepository(tempDirectory, "cloudflare-");
    }

    @After
    public void cleanUpTempDir() throws IOException {
        FileVisitor<Path> cleaner = new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return null;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(tempDirectory, EnumSet.of(FOLLOW_LINKS), 1, cleaner);
    }

    @Test
    public void returns_null_when_loading_missing_module() {
        //given an empty dir

        //when asked to read the module
        TerraformModule readModule = repository.getByModuleId("instance_id");

        assertThat(readModule).isNull();
    }

    @Test
    public void saves_and_load_modules() throws IOException {
        //given an empty dir

        //when
        ImmutableTerraformModule module = aModule("instance_id");
        repository.save(module);

        //then it actually writes the file
        Gson gson = new GsonBuilder().registerTypeAdapter(ImmutableTerraformModule.class, new TerraformModuleGsonAdapter()).create();
        FileReader reader = new FileReader(tempDirectory.resolve("cloudflare-instance_id.tf").toFile());
        ImmutableTerraformModule readFromDisk = gson.fromJson(reader, ImmutableTerraformModule.class);
        ImmutableTerraformModule actual = ImmutableTerraformModule.builder()
                .from(readFromDisk)
                .id(module.getId()) //override id
                .build();
        assertThat(actual).isEqualTo(module);

        //when asked to read the module
        TerraformModule readModule = repository.getByModuleId("instance_id");

        //then
        assertThat(readModule).isEqualTo(module);
    }

    @Test
    public void list_modules() throws IOException {
        //Given a directory holding tf modules
        list_modules_with_prefix("cloudflare-");
        list_modules_with_prefix("");
        list_modules_with_prefix(null);
    }

    @Test
    public void finds_modules_by_property() {
        //Given
        ImmutableTerraformModule module1 = ImmutableTerraformModule.builder()
                .moduleName("0")
                .source("path/to/module")
                .id("0")
                .putProperties("prop1", "value1")
                .build();
        ImmutableTerraformModule module2 = ImmutableTerraformModule.builder()
                .moduleName("1")
                .source("path/to/module")
                .putProperties("prop1", "value1")
                .id("1")
                .build();
        repository.save(module1);
        repository.save(module2);
        List<ImmutableTerraformModule> potentials = asList(module1, module2);

        //when
        TerraformModule module = repository.getByModuleProperty("prop1", "value1");

        //then
        assertThat(potentials).contains(module);
    }

    @Test
    public void deletes_modules() {
        //given
        ImmutableTerraformModule module = aModule("instance_id");
        repository.save(module);

        //when
        repository.delete(module);

        //then
        assertThat(repository.getByModuleId(module.getId())).isNull();

    }

    private void list_modules_with_prefix(String filePrefix) throws IOException {
        //Given
        FileTerraformRepository repository = new FileTerraformRepository(tempDirectory, filePrefix);
        ImmutableTerraformModule module1 = aModule("0");
        ImmutableTerraformModule module2 = aModule("1");
        repository.save(module1);
        repository.save(module2);

        //when
        List<ImmutableTerraformModule> modules = repository.findAll();

        //then
        assertThat(modules).containsOnly(module1, module2);
    }


    public ImmutableTerraformModule aModule(String id) {
        return ImmutableTerraformModule.builder()
                .moduleName(id)
                .source("path/to/module")
                .id(id)
                .build();
    }

}