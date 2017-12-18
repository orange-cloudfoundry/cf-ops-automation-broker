package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;

/**
 * Created by ijly7474 on 18/12/17.
 */
public class StructureGeneratorHelper {

    public static Path generatePath(Path rootPath, String... pathElements){
        Path generatedPath = rootPath;
        for ( String s : pathElements )
            generatedPath = generatedPath.resolve(s);
        return generatedPath;
    }
}
