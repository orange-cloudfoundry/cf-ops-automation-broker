package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public class CopyTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test() {
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        try {
            //source
            URL resource = this.getClass().getResource("/sample-deployment-model");
            Path referenceDataModel = Paths.get(resource.toURI());

            //destination
/*
            if (pathElements != null){
                for ( String s : pathElements )
                    generatedPath = generatedPath.resolve(s);
                Path generatedPath = rootPath;
            }
            return generatedPath;
            Path dest = (isDir) ? target.resolve(source[i].getFileName()) : target;
*/


            Path target = temporaryFolder.getRoot().toPath();


            //recursive
            Copy.TreeCopier tc = new Copy.TreeCopier(referenceDataModel, target, "coab-depls", false, true);
            Files.walkFileTree(referenceDataModel, opts, Integer.MAX_VALUE, tc);

            //print result
            new Tree().print(target);



        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
