package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.StructureGeneratorHelper;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;

public class TreeTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void test() throws IOException {
        //Given a root path and path elements to create
        Path rootPath = this.temporaryFolder.getRoot().toPath();
        Path realFilePath = rootPath.resolve("aRealFile.txt");
        Files.createFile(realFilePath);
        Path fakeFilePath = rootPath.resolve("aFakeFile.txt");
        Files.createFile(fakeFilePath);

        //When
        StructureGeneratorHelper.generateSymbolicLink(rootPath, null, null, "aRealFile.txt", "linkToARealFile.txt");
        StructureGeneratorHelper.generateSymbolicLink(rootPath, null, null, "aFakeFile.txt", "linkToAFakeFile.txt");
        Files.deleteIfExists(fakeFilePath);


        System.out.println((new Tree()).print(rootPath));
    }

}
