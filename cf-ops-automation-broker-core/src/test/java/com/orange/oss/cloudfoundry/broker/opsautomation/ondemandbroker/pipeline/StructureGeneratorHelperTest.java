package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by ijly7474 on 18/12/17.
 */
public class StructureGeneratorHelperTest {

    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";

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

    @Test
    public void check_find_and_replace(){
        //Given an array list and a map
        List<String> lines = new ArrayList<String>();
        lines.add("---");
        lines.add("deployment:");
        lines.add("  @service_instance@:");
        lines.add("  value: @service_instance@");
        lines.add("  value: @url@.((!/secrets/cloudfoundry_system_domain))");
        Map<String, String> map = new HashMap<String, String>();
        map.put(CassandraProcessorConstants.SERVICE_INSTANCE_PATTERN, CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
        map.put(CassandraProcessorConstants.URL_PATTERN, CassandraProcessorConstants.BROKER_PREFIX + SERVICE_INSTANCE_ID);

        //When
        List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, map);

        //Then
        assertEquals("---", (String)resultLines.get(0));
        assertEquals("deployment:", (String)resultLines.get(1));
        assertEquals("  cassandra_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0:", (String)resultLines.get(2));
        assertEquals("  value: cassandra_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0", (String)resultLines.get(3));
        assertEquals("  value: cassandra-broker_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0.((!/secrets/cloudfoundry_system_domain))", (String)resultLines.get(4));
    }
}
