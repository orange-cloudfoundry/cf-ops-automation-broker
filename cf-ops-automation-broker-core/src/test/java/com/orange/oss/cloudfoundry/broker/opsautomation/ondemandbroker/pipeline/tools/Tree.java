package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Tree {
    private int dirCount;
    private int fileCount;

    public Tree() {
        this.dirCount = 0;
        this.fileCount = 0;
    }

    public void print(String directory) {
        //System.out.println(directory);
        System.out.println(".");
        try {
            walk(new File(directory), "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n" + this.dirCount + " directories, " + this.fileCount + " files");
    }

    private void register(File file) {
        if (file.isDirectory()) {
            this.dirCount += 1;
        } else {
            this.fileCount += 1;
        }
    }

    private static boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
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
                if (isSymlink(file)){
                    Path filePath = Paths.get(file.getCanonicalPath());
                    Path linkPath = Paths.get(file.toPath().toString());
                    System.out.println(prefix + "└── " + linkPath.relativize(filePath));
                }else{
                    System.out.println(prefix + "└── " + file.getName());
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "    ");
                }
            } else {
                if (isSymlink(file)){
                    Path filePath = Paths.get(file.getCanonicalPath());
                    Path linkPath = Paths.get(file.toPath().toString());
                    System.out.println(prefix + "└── " + linkPath.relativize(filePath));
                }else{
                    System.out.println(prefix + "└── " + file.getName());
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "│   ");
                }
            }
        }
    }
}
