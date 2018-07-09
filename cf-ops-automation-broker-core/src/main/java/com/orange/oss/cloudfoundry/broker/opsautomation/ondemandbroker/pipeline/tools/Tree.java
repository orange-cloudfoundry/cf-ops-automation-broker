package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Tree {
    private int dirCount;
    private int fileCount;
    private StringBuffer result;

    public Tree() {
        this.dirCount = 0;
        this.fileCount = 0;
        this.result = new StringBuffer();
    }

    public String print(Path path) {
        //System.out.println(path.getFileName());
        this.result.append(path.getFileName() + System.getProperty("line.separator"));
        try {
            walk(new File(path.toString()), "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("\n" + this.dirCount + " directories, " + this.fileCount + " files");
        return this.result.toString();
    }

    private void register(File file) {
        if (file.isDirectory()) {
            this.dirCount += 1;
        } else {
            this.fileCount += 1;
        }
    }

    private void walk(File folder, String prefix) throws IOException{
        File file;
        File[] fileList = folder.listFiles();
        Arrays.sort(fileList);

        for (int index = 0; index < fileList.length; index++) {
            file = fileList[index];
            if (file.getName().charAt(0) == '.') {
                continue;
            }
            register(file);

            if (index == fileList.length - 1) {
                if (Files.isSymbolicLink(file.toPath())){
                    this.result.append(prefix + "└── " + file.getName()+ " -> " + Files.readSymbolicLink(file.toPath()) + System.getProperty("line.separator"));

                }else{
                    this.result.append(prefix + "└── " + file.getName() + System.getProperty("line.separator"));
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "    ");
                }
            } else {
                if (Files.isSymbolicLink(file.toPath())){
                    this.result.append(prefix + "└── " + file.getName()+ " -> " + Files.readSymbolicLink(file.toPath()) + System.getProperty("line.separator"));
                }else{
                    this.result.append(prefix + "└── " + file.getName() + System.getProperty("line.separator"));
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "│   ");
                }
            }
        }
    }
}
