package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class StructureGeneratorHelperTest {


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void check_generated_path() {
        //Given a root path and path elements
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        String element1 = "element1";
        String element2 = "element2";
        String element3 = "element3";

        //When
        Path path = StructureGeneratorHelper.generatePath(rootPath, element1, element2, element3);
        String actual = String.valueOf(path);

        //Then
        StringBuffer sb = new StringBuffer(String.valueOf(rootPath));
        sb.append(File.separator)
                .append(element1).append(File.separator)
                .append(element2).append(File.separator)
                .append(element3);
        String expected = sb.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void check_generate_directory() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        String aDirectory = "directory";

        //When
        StructureGeneratorHelper.generateDirectory(rootPath, aDirectory);

        //Then
        Path expectedPath = rootPath.resolve("directory");
        assertThat("Generate directory doesn't exist", Files.exists(expectedPath));
    }

    @Test
    public void check_generate_symbolic_link() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();

        //When

        //Then


    }

    @Test
    public void check_generate_file() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        try {
            Path path = this.temporaryFolder.newFolder("aPath").toPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        StructureGeneratorHelper.generateFile(rootPath, new String[]{"aPath"}, "aTargetFile", DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME, null);

        //Then
        Path expectedPath = rootPath.resolve("aPath").resolve("aTargetFile");
        assertThat("File doesn't exist", Files.exists(expectedPath));
    }

    @Test
    public void check_remove_file() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        try {
            Path path = this.temporaryFolder.newFolder("aPath").toPath();
            path = path.resolve("aFile");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        Path expectedPath = rootPath.resolve("aPath").resolve("aFile");
        assertThat("File exists", Files.exists(expectedPath));
        StructureGeneratorHelper.removeFile(rootPath, new String[]{"aPath"}, "aFile");

        //Then
        assertThat("File still exists", Files.notExists(expectedPath));
    }


    @Test
    public void check_find_and_replace() {
        //Given a template with markers
        List<String> lines = new ArrayList<String>();
        lines.add("---");
        lines.add("deployment:");
        lines.add("  @service_instance@:");
        lines.add("  value: @service_instance@");
        lines.add("  value: @url@.((!/secrets/cloudfoundry_system_domain))");

        //When asking to replace some markers
        Map<String, String> map = new HashMap<String, String>();
        map.put("@service_instance@", "c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        map.put("@url@", "cassandra-broker_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, map);

        //Then
        assertEquals("---", resultLines.get(0));
        assertEquals("deployment:", resultLines.get(1));
        assertEquals("  c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0:", resultLines.get(2));
        assertEquals("  value: c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0", resultLines.get(3));
        assertEquals("  value: cassandra-broker_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0.((!/secrets/cloudfoundry_system_domain))", resultLines.get(4));
    }

    @Test
    public void check_list_files_paths_manifest() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        try {
            Path path = rootPath.resolve("model-vars.yml");
            Files.createFile(path);
            path = rootPath.resolve("model-tpl.yml");
            Files.createFile(path);
            path = rootPath.resolve("model.yml");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "{model.yml,model-tpl.yml}");

        //Then
        assertThat("model.yml is not present", paths.contains("model.yml"));
        assertThat("model-tpl.yml is not present", paths.contains("model-tpl.yml"));
        assertThat("model-vars.yml is present", ! paths.contains("model-vars.yml"));
    }

    @Test
    public void check_list_files_paths_vars() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        try {
            Path path = rootPath.resolve("model-vars.yml");
            Files.createFile(path);
            path = rootPath.resolve("model-vars-tpl.yml");
            Files.createFile(path);
            path = rootPath.resolve("model.yml");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "{model-vars.yml,model-vars-tpl.yml}");

        //Then
        assertThat("model-vars.yml is not present", paths.contains("model-vars.yml"));
        assertThat("model-vars-tpl.yml is not present", paths.contains("model-vars-tpl.yml"));
        assertThat("model.yml is present", ! paths.contains("model.yml"));
    }

    @Test
    public void check_list_files_paths_operators() {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        try {
            Path path = rootPath.resolve("file-operators.yml");
            Files.createFile(path);
            path = rootPath.resolve("file-op.yml");
            Files.createFile(path);
            path = rootPath.resolve("file.yml");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "*-operators.yml");

        //Then
        assertThat("file-operators.yml is not present", paths.contains("file-operators.yml"));
        assertThat("file-op.yml is present", ! paths.contains("file-op.yml"));
        assertThat("file.yml is present", ! paths.contains("file.yml"));
    }

}