package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

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
        FileWriter fileWriter = new FileWriter(tempDirectory.resolve("a-static-tf-config-to-be-ignored.tf").toFile());
        fileWriter.write("Please ignore me, you should not try to parse me");
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void constructor_rejects_null_dir() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("null"));

        new FileTerraformRepository(null, "a prefix");
    }

    @Test
    public void constructor_rejects_missing_dir() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("does not exist"));
        
        new FileTerraformRepository(Paths.get("/nosuchpath"), "a prefix");
    }

    @Test
    public void returns_null_when_loading_missing_module() {
        //given an empty dir

        //when asked to read the module
        TerraformModule readModule = repository.getByModuleName("instance_id");

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
        ImmutableTerraformModule actual = gson.fromJson(reader, ImmutableTerraformModule.class);
        assertThat(actual).isEqualTo(module);

        //when asked to read the module
        TerraformModule readModule = repository.getByModuleName("instance_id");

        //then
        assertThat(readModule).isEqualTo(module);
    }

    @Test
    public void pretty_prints_module_outputs() throws IOException {
        //when
        ImmutableTerraformModule module = aModule("instance_id");
        repository.save(module);

        //then it actually writes the file
        FileReader reader = new FileReader(tempDirectory.resolve("cloudflare-instance_id.tf").toFile());

        String json = IOUtils.toString(reader);
        assertThat(json).isEqualTo(
                "{\n" +
                        "  \"module\": {\n" +
                        "    \"instance_id\": {\n" +
                        "      \"source\": \"path/to/module\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");
    }

    @Test(expected = RuntimeException.class)
    public void rejects_module_names_injections() {
        repository.buildFileForModule("../asuffix");
    }

    @Test
    public void list_modules() throws IOException {
        //Given a directory holding tf modules
        FileTerraformRepository repository1 = new FileTerraformRepository(tempDirectory, "cloudflare-");
        ImmutableTerraformModule module1 = aModule("0");
        ImmutableTerraformModule module2 = aModule("1");
        repository1.save(module1);
        repository1.save(module2);

        //when
        List<ImmutableTerraformModule> modules = repository1.findAll();

        //then
        assertThat(modules).containsOnly(module1, module2);
    }

    @Test
    public void finds_modules_by_property() {
        //Given
        ImmutableTerraformModule module1 = ImmutableTerraformModule.builder()
                .moduleName("0")
                .source("path/to/module")
                .putProperties("prop1", "value1")
                .build();
        ImmutableTerraformModule module2 = ImmutableTerraformModule.builder()
                .moduleName("1")
                .source("path/to/module")
                .putProperties("prop1", "value1")
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
        assertThat(repository.getByModuleName(module.getModuleName())).isNull();

    }


    public ImmutableTerraformModule aModule(String id) {
        return ImmutableTerraformModule.builder()
                .moduleName(id)
                .source("path/to/module")
                .build();
    }

}