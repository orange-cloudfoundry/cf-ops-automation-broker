package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

public class TreeNew {
/*
    private int dirCount;
    private int fileCount;

    public TreeNew() {
        this.dirCount = 0;
        this.fileCount = 0;
    }

    public void print(String directory) {
        System.out.println(directory);
        walk(new File(directory), "");
        System.out.println("\n" + this.dirCount + " directories, " + this.fileCount + " files");
    }

    private void register(File file) {
        if (file.isDirectory()) {
            this.dirCount += 1;
        } else {
            this.fileCount += 1;
        }
    }

    private void walk(Path path, String prefix) {
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{**}");

        try{
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path,
                                                 BasicFileAttributes attrs) {
                    if (pathMatcher.matches(path)) {
                        paths.add(path.getFileName().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

        }catch (IOException e) {
            e.printStackTrace();
            throw new DeploymentException(DeploymentConstants.SEARCH_EXCEPTION);
        }
        return paths;
    }

*/

}