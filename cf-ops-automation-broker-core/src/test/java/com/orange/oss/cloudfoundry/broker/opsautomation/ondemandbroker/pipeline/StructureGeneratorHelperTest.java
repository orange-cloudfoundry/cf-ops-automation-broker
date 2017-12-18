package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Created by ijly7474 on 18/12/17.
 */
public class StructureGeneratorHelperTest {

    @Test
    public void check_generated_path(){
            //Given a root path and path elements
            Path rootPath = Paths.get("/tmp");
            String element1 = "element1";
            String element2 = "element2";
            String element3 = "element3";

            //When
            Path path = StructureGeneratorHelper.generatePath(rootPath, element1, element2, element3);
            String actual = String.valueOf(path);

            //Then
            StringBuffer sb = new StringBuffer(String.valueOf(rootPath));
            sb.append(File.separator)
                    .append(element1).append(File.separator)
                    .append(element2).append(File.separator)
                    .append(element3);
            String expected = sb.toString();
            assertEquals(expected, actual);
    }
}
