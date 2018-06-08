package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

public class VarsFilesYmlFormatterTest {

    private static Logger logger= LoggerFactory.getLogger(VarsFilesYmlFormatterTest.class.getName());


    @Test
    public void collections_outputs_expected_vars_files() throws JsonProcessingException {
        HashMap<String, Object> varsMap = new HashMap<>();
        varsMap.put("deployment_name", "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        HashMap<String, Object> cloudfoundry = new HashMap<>();
        cloudfoundry.put("user_guid", "user_guid1");
        cloudfoundry.put("space_guid", "space_guid1");
        cloudfoundry.put("org_guid", "org_guid1");
        varsMap.put("cloudfoundry", cloudfoundry);

        HashMap<String, Object> params = new HashMap<>();
        params.put("slowQuery", "false");
        params.put("cacheSizeMb", "10");
        varsMap.put("params", params);


        ObjectMapper mapper=new ObjectMapper(new YAMLFactory());
        String result = mapper.writeValueAsString(varsMap);
        logger.info("vars.yml serialized yml content:\n{}", result);
        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "cloudfoundry:\n" +
                        "  org_guid: \"org_guid1\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "params:\n" +
                        "  cacheSizeMb: \"10\"\n" +
                        "  slowQuery: \"false\"\n");
    }

    public static class CoabVarsFile {
        public String deployment_name;
        public HashMap<String, String> cloudfoundry = new HashMap<>();
        public HashMap<String, String> params = new HashMap<>();
    }

    @Test
    public void pojo_outputs_expected_vars_files() throws JsonProcessingException {
        CoabVarsFile coabVarsFile = new CoabVarsFile();
        coabVarsFile.deployment_name= "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFile.cloudfoundry.put("user_guid", "user_guid1");
        coabVarsFile.cloudfoundry.put("space_guid", "space_guid1");
        coabVarsFile.cloudfoundry.put("org_guid", "org_guid1");

        coabVarsFile.params.put("slowQuery", "false");
        coabVarsFile.params.put("cacheSizeMb", "10");


        ObjectMapper mapper=new ObjectMapper(new YAMLFactory());
        String result = mapper.writeValueAsString(coabVarsFile);
        logger.info("vars.yml serialized yml content:\n{}", result);

        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "cloudfoundry:\n" +
                        "  org_guid: \"org_guid1\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "params:\n" +
                        "  cacheSizeMb: \"10\"\n" +
                        "  slowQuery: \"false\"\n");
    }

}
