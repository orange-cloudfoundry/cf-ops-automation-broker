package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static List<String> findAndReplace(List<String> lines, Map<String, String> map){
        List<String> resultLines = new ArrayList<String>();
        for (String s : lines){
            String resultLine = null;
            for (Map.Entry mapentry : map.entrySet()) {
                resultLine = s.replaceFirst((String)mapentry.getKey(), (String)mapentry.getValue());
                if (!s.equals(resultLine)){
                    break;
                }
            }
            resultLines.add(resultLine);
        }
        return resultLines;
    }
}
