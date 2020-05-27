package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.StructureGeneratorHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TreeTest {

    @TempDir
    File tempDir;


    @Test
    public void test() throws IOException {
        //Given a root path and path elements to create
        Path rootPath = tempDir.toPath();
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
