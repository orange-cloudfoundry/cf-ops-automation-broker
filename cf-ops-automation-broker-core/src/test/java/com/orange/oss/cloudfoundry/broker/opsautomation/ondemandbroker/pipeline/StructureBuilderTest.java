package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StructureBuilderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void builder_test() {

        Structure structure = new Structure.StructureBuilder(temporaryFolder.getRoot().toPath())
                //.withDirectoryHierarchy("coab-depls", "mongodb")
                .withFile(new String[]{"coab-depls", "mongodb"}, "file")
                .build();
     }


}
