package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

public class StructureBuilderTest {

    @Rule
    public TemporaryFolder temporaryFolder;

    @BeforeEach
    void setUp() throws IOException {
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
    }

    @Test
    public void builder_test() {

        Structure structure = new Structure.StructureBuilder(temporaryFolder.getRoot().toPath())
                //.withDirectoryHierarchy("coab-depls", "mongodb")
                .withFile(new String[]{"coab-depls", "mongodb"}, "file")
                .build();
     }


}
