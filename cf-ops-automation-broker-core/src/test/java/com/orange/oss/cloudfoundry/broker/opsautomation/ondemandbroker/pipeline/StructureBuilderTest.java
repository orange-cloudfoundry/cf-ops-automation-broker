package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StructureBuilderTest {

    @TempDir
    File tempDir;

    @Test
    public void builder_test() {

        Structure structure = new Structure.StructureBuilder(tempDir.toPath())
                //.withDirectoryHierarchy("coab-depls", "mongodb")
                .withFile(new String[]{"coab-depls", "mongodb"}, "file")
                .build();
     }


}
