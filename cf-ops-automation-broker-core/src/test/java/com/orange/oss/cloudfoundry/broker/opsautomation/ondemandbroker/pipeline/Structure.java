package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Structure {

    // required parameters
    private Path rootDirectory;

    private Structure(StructureBuilder builder) {
    }

    // builder Class
    public static class StructureBuilder{

        // required parameters
        private Path rootDirectory;

        public StructureBuilder(Path rootDirectory){
            this.rootDirectory=rootDirectory;
        }

        public StructureBuilder withDirectoryHierarchy(String ...directories) {
            Path path = StructureGeneratorHelper.generatePath(this.rootDirectory, directories);
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        public StructureBuilder withFile(String[] directories, String file) {
            Path directoryPath = StructureGeneratorHelper.generatePath(this.rootDirectory, directories);
            Path filePath = StructureGeneratorHelper.generatePath(directoryPath, file);
            try {
                Files.createDirectories(directoryPath);
                Files.createFile(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Structure build(){
            return new Structure(this);
        }
    }
}


