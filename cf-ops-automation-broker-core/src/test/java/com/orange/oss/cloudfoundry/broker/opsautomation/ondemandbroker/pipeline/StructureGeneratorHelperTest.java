package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;



public class StructureGeneratorHelperTest {


    @TempDir
    File temporaryFolderRoot;


    @Test
    public void check_generated_path() {
        //Given a root path and path elements
        Path rootPath = temporaryFolderRoot.toPath();
        String element1 = "element1";
        String element2 = "element2";
        String element3 = "element3";

        //When
        Path path = StructureGeneratorHelper.generatePath(rootPath, element1, element2, element3);
        String actual = String.valueOf(path);

        //Then
        String expected = rootPath + File.separator +
            element1 + File.separator +
            element2 + File.separator +
            element3;
        assertEquals(expected, actual);
    }

    @Test
    public void check_generate_directory() {
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        String aDirectory = "directory";

        //When
        StructureGeneratorHelper.generateDirectory(rootPath, aDirectory);

        //Then
        Path expectedPath = rootPath.resolve("directory");
        assertThat("Generate directory doesn't exist", Files.exists(expectedPath));
    }

    @Test
    public void check_generate_symbolic_link_to_a_real_file() throws IOException{
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        Path filePath = rootPath.resolve("aRealFile.txt");
        Files.createFile(filePath);

        //When
        StructureGeneratorHelper.generateSymbolicLink(rootPath, null, null, "aRealFile.txt", "linkToARealFile.txt");

        //Then
        Path expectedSymbolicLink = rootPath.resolve("linkToARealFile.txt");
        assertThat("Symbolic link doesn't exist", Files.exists(expectedSymbolicLink));
        assertThat("File is not a symbolic link", Files.isSymbolicLink(expectedSymbolicLink));
        assertThat(Files.readSymbolicLink(expectedSymbolicLink).toString(), is(equalTo("aRealFile.txt")));
    }

    @Test
    public void check_generate_symbolic_link_to_a_fake_file() {
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        Path filePath = rootPath.resolve("aFakeFile.txt");

        //When
        StructureGeneratorHelper.generateSymbolicLink(rootPath, null, null, "aFakeFile.txt", "linkToAFakeFile.txt");

        //Then
        Path expectedSymbolicLink = rootPath.resolve("linkToAFakeFile.txt");
        assertThat("Symbolic link exist", Files.notExists(expectedSymbolicLink, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void check_generate_symbolic_link_with_existing_target_file_do_not_fail() throws IOException{
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        Path filePath = rootPath.resolve("aRealFile.txt");
        Files.createFile(filePath);
        Path existingFilePath = rootPath.resolve("linkToAFakeFile.txt");
        Files.createFile(existingFilePath);

        //When
        StructureGeneratorHelper.generateSymbolicLink(rootPath, null, null, "aRealFile.txt", "linkToAFakeFile.txt");

        //Then (no exception java.nio.file.FileAlreadyExistsException)
    }




    @Test
    public void check_generate_file() {
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        //noinspection ResultOfMethodCallIgnored
        rootPath.resolve("aPath").toFile().mkdir();

        //When
        StructureGeneratorHelper.generateFile(rootPath, new String[]{"aPath"}, "aTargetFile", DeploymentConstants.ENABLE_DEPLOYMENT_FILENAME, null);

        //Then
        Path expectedPath = rootPath.resolve("aPath").resolve("aTargetFile");
        assertThat("File doesn't exist", Files.exists(expectedPath));
    }

    @Test
    public void check_remove_file() {
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        Path aPath = rootPath.resolve("aPath");
        try {
            //noinspection ResultOfMethodCallIgnored
            aPath.toFile().mkdir();
            Files.createFile(aPath.resolve("aFile"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        Path expectedPath = aPath.resolve("aFile");
        assertThat("File exists", Files.exists(expectedPath));
        StructureGeneratorHelper.removeFile(rootPath, new String[]{"aPath"}, "aFile");

        //Then
        assertThat("File still exists", Files.notExists(expectedPath));
    }


    @Test
    public void check_find_and_replace() {
        //Given a template with markers
        List<String> lines = new ArrayList<>();
        lines.add("---");
        lines.add("deployment:");
        lines.add("  @service_instance@:");
        lines.add("  value: @service_instance@");
        lines.add("  value: @url@.((!/secrets/cloudfoundry_system_domain))");

        //When asking to replace some markers
        Map<String, String> map = new HashMap<>();
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
        Path rootPath = temporaryFolderRoot.toPath();
        try {
            Path directoryPath = rootPath.resolve(DeploymentConstants.TEMPLATE);
            Files.createDirectory(directoryPath);
            Path path = directoryPath.resolve("model-vars.yml");
            Files.createFile(path);
            path = directoryPath.resolve("model-tpl.yml");
            Files.createFile(path);
            path = directoryPath.resolve("model.yml");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "glob:{**/model.yml,**/model-tpl.yml}");

        //Then
        assertThat("model.yml is not present", paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model.yml"));
        assertThat("model-tpl.yml is not present", paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model-tpl.yml"));
        assertThat("model-vars.yml is present", ! paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model-vars.yml"));
    }

    @Test
    public void check_list_files_paths_vars() {
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        try {
            Path directoryPath = rootPath.resolve(DeploymentConstants.TEMPLATE);
            Files.createDirectory(directoryPath);
            Path path = directoryPath.resolve("model-vars.yml");
            Files.createFile(path);
            path = directoryPath.resolve("model-vars-tpl.yml");
            Files.createFile(path);
            path = directoryPath.resolve("model.yml");
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "glob:{**/model-vars.yml,**/model-vars-tpl.yml}");

        //Then
        assertThat("model-vars.yml is not present", paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model-vars.yml"));
        assertThat("model-vars-tpl.yml is not present", paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model-vars-tpl.yml"));
        assertThat("model.yml is present", ! paths.contains(DeploymentConstants.TEMPLATE + File.separator + "model.yml"));
    }

    @Test
    public void check_list_files_paths_operators() throws IOException{
        //Given a root path and path elements to create
        Path rootPath = temporaryFolderRoot.toPath();
        Path directoryPath = rootPath.resolve(DeploymentConstants.TEMPLATE);
        Files.createDirectory(directoryPath);
        Path path = directoryPath.resolve("file-operators.yml");
        Files.createFile(path);
        path = directoryPath.resolve("file-op.yml");
        Files.createFile(path);
        path = directoryPath.resolve("file.yml");
        Files.createFile(path);

        //When
        List<String> paths = StructureGeneratorHelper.listFilesPaths(rootPath, "glob:{**/*-operators.yml}");

        //Then
        assertThat("file-operators.yml is not present", paths.contains(DeploymentConstants.TEMPLATE + File.separator + "file-operators.yml"));
        assertThat("file-op.yml is present", ! paths.contains(DeploymentConstants.TEMPLATE + File.separator + "file-op.yml"));
        assertThat("file.yml is present", ! paths.contains(DeploymentConstants.TEMPLATE + File.separator + "file.yml"));
    }

    @Test
    public void check_get_directory() {
        //Given

        //When

        //Then
    }

    @Test
    public void check_get_file() {
        //Given

        //When

        //Then
    }

}