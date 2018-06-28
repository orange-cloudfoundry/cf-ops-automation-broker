package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import java.io.File;
import java.io.IOException;
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

    private static Path computeRelativePath(File file) throws IOException{
        Path filePath = Paths.get(file.getCanonicalFile().getParent());
        Path linkPath = Paths.get(file.getParent());
        Path relativePath = linkPath.relativize(filePath);
        return relativePath.resolve(file.getCanonicalFile().getName());
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
                    //System.out.println(prefix + "└── " + computeRelativePath(file));
                    this.result.append(prefix + "└── " + computeRelativePath(file) + System.getProperty("line.separator"));

                }else{
                    //System.out.println(prefix + "└── " + file.getName());
                    this.result.append(prefix + "└── " + file.getName() + System.getProperty("line.separator"));
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "    ");
                }
            } else {
                if (isSymlink(file)){
                    //System.out.println(prefix + "└── " + computeRelativePath(file));
                    this.result.append(prefix + "└── " + computeRelativePath(file) + System.getProperty("line.separator"));
                }else{
                    //System.out.println(prefix + "└── " + file.getName());
                    this.result.append(prefix + "└── " + file.getName() + System.getProperty("line.separator"));
                }
                if (file.isDirectory()) {
                    walk(file, prefix + "│   ");
                }
            }
        }
    }
}
