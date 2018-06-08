package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

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


        String result = formatAsYml(varsMap);
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

    protected String formatAsYml(Object o) throws JsonProcessingException {
        ObjectMapper mapper=new ObjectMapper(new YAMLFactory());
        return mapper.writeValueAsString(o);
    }

    public static class CoabVarsFile {
        @JsonProperty("deployment_name")
        public String deployment_name;
        @JsonProperty("service_id")
        public String service_id;
        @JsonProperty("plan_id")
        public String plan_id;

        @JsonProperty("context")
        public CloudFoundryOsbContext context = new CloudFoundryOsbContext();
        @JsonProperty("parameters")
        public HashMap<String, String> parameters = new HashMap<>();

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("previous_values")
        public PreviousValues previous_values;

    }

    public static class CloudFoundryOsbContext {
        public String platform = "cloudfoundry";
        public String user_guid;
        public String space_guid;
        public String organization_guid;
    }

    public static class PreviousValues {
        @NotEmpty
        @JsonSerialize
        public String plan_id;
    }


    @Test
    public void create_dto_outputs_expected_vars_files() throws JsonProcessingException {
        CoabVarsFile coabVarsFile = new CoabVarsFile();
        coabVarsFile.deployment_name= "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFile.service_id= "service_definition_id";
        coabVarsFile.plan_id= "plan_guid";

        coabVarsFile.context.user_guid="user_guid1";
        coabVarsFile.context.space_guid="space_guid1";
        coabVarsFile.context.organization_guid="org_guid1";


        coabVarsFile.parameters.put("slowQuery", "false");
        coabVarsFile.parameters.put("cacheSizeMb", "10");


        String result = formatAsYml(coabVarsFile);
        logger.info("vars.yml serialized yml content:\n{}", result);

        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "service_id: \"service_definition_id\"\n" +
                        "plan_id: \"plan_guid\"\n" +
                        "context:\n" +
                        "  platform: \"cloudfoundry\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "  organization_guid: \"org_guid1\"\n" +
                        "parameters:\n" +
                        "  cacheSizeMb: \"10\"\n" +
                        "  slowQuery: \"false\"\n");
    }

    @Test
    public void update_dto_outputs_expected_vars_files() throws JsonProcessingException {
        CoabVarsFile coabVarsFile = new CoabVarsFile();
        coabVarsFile.deployment_name= "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFile.service_id= "service_definition_id";
        coabVarsFile.plan_id= "plan_guid";

        coabVarsFile.context.user_guid="user_guid1";
        coabVarsFile.context.space_guid="space_guid1";
        coabVarsFile.context.organization_guid="org_guid1";

        coabVarsFile.previous_values = new PreviousValues();
        coabVarsFile.previous_values.plan_id= "previous_plan_guid";

        coabVarsFile.parameters.put("slowQuery", "false");
        coabVarsFile.parameters.put("cacheSizeMb", "10");


        String result = formatAsYml(coabVarsFile);
        logger.info("vars.yml serialized yml content:\n{}", result);

        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "service_id: \"service_definition_id\"\n" +
                        "plan_id: \"plan_guid\"\n" +
                        "context:\n" +
                        "  platform: \"cloudfoundry\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "  organization_guid: \"org_guid1\"\n" +
                        "parameters:\n" +
                        "  cacheSizeMb: \"10\"\n" +
                        "  slowQuery: \"false\"\n" +
                        "previous_values:\n" +
                        "  plan_id: \"previous_plan_guid\"\n");
    }

    private static final String SERVICE_DEFINITION_ID = "cassandra-ondemand-service";
    private static final String SERVICE_PLAN_ID = "cassandra-ondemand-plan";
    private static final String SERVICE_INSTANCE_ID = "111";
    private static final String SERVICE_BINDING_INSTANCE_ID = "222";


    private CreateServiceInstanceRequest aCreateServiceInstanceRequest() {

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID,
                SERVICE_PLAN_ID,
                "org_id",
                "space_id",
                aCfOsbContext(),
                new HashMap<>()
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        return request;
    }

    private org.springframework.cloud.servicebroker.model.Context aCfOsbContext() {
        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id");
        return new org.springframework.cloud.servicebroker.model.Context(
                CLOUD_FOUNDRY_PLATFORM,
                contextProperties
        );
    }



    @Test
    public void spring_cloud_broker_dtos_outputs_expected_yml() throws JsonProcessingException {

        String result = formatAsYml(aCreateServiceInstanceRequest());
        assertThat(result).isEqualTo("---\n" +
                "asyncAccepted: false\n" +
                "parameters: {}\n" +
                "context: !<Context>\n" +
                "  platform: \"cloudfoundry\"\n" +
                "service_id: \"cassandra-ondemand-service\"\n" +
                "plan_id: \"cassandra-ondemand-plan\"\n" +
                "organization_guid: \"org_id\"\n" +
                "space_guid: \"space_id\"\n" +
                "");
    }

}
