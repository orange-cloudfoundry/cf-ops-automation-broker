package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

public class VarsFilesYmlFormatterTest {

    VarsFilesYmlFormatter formatter = new VarsFilesYmlFormatter();

    private static Logger logger = LoggerFactory.getLogger(VarsFilesYmlFormatterTest.class.getName());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_invalid_patterns() {
        assertStringRejected("((");
        assertStringRejected("(( a/string ))");
        assertStringRejected("))");


        assertStringAccepted("a string with spaces");
        assertStringAccepted("10mb");
        assertStringAccepted("a deployment model cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        assertStringAccepted("org_guid1");
        assertStringAccepted("c_5f89138b-ed9a-4596-a042-a6349b6a1f04 ");
        assertStringAccepted("cacheSizeMb");
        assertStringAccepted("10");

    }

    protected void assertStringRejected(String yml) {
        //noinspection EmptyCatchBlock
        try {
            formatter.rejectUnsupportedPatterns(yml);
            fail("expected " + yml + "to be rejected");
        } catch (UserFacingRuntimeException e) {

        }
    }

    protected void assertStringAccepted(String yml) {
        formatter.rejectUnsupportedPatterns(yml);
    }


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


        String result = formatter.formatAsYml(varsMap);
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


    protected CoabVarsFileDto aTypicalUserRequest() {
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";
        return coabVarsFileDto;
    }


    @Test
    public void create_dto_outputs_expected_vars_files() throws IOException {
        //Given
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";


        coabVarsFileDto.parameters.put("slowQuery", false);
        coabVarsFileDto.parameters.put("cacheSizeMb", 10);
        coabVarsFileDto.parameters.put("apiKey", "A STRING with escaped quotes \" and escaped & references");

        //when
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
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
                        "  apiKey: \"A STRING with escaped quotes \\\" and escaped & references\"\n" +
                        "  cacheSizeMb: 10\n" +
                        "  slowQuery: false\n");

        //and potentially in the future parse back from yml, e.g. for OSB get endpoints
        Object deserialized = parseFromYml(result, coabVarsFileDto.getClass());
        //potentially check that both are identitical (need to debug reflection equals)
        //        assertThat(reflectionEquals(coabVarsFileDto, deserialized)).isTrue();
    }

    protected Object parseFromYml(String result, Class expectedClass) throws IOException {
        Object deserialized = formatter.getMapper().readValue(result, expectedClass);
        assertThat(deserialized).isNotNull();
        return deserialized;

    }

    static class VulnerableBean {
        public int id;
        public Object obj;
    }

    /**
     * Exploration of malicous injection of untrusted code from JSON/YML param injection
     */
    @Test
    @Ignore
    public void reading_malicious_handcrafted_json_should_reject_unknown_classes() throws IOException {

        //given a malicious yml provided by users are unfiltered plain text arbitrary params of their service instance
        final String JSON = (
                "{'id': 124,\n"
                        + " 'obj':[ 'com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl',\n"
                        + "  {\n"
                        + "    'transletBytecodes' : [ 'AAIAZQ==' ],\n"
                        + "    'transletName' : 'a.b',\n"
                        + "    'outputProperties' : { }\n"
                        + "  }\n"
                        + " ]\n"
                        + "}").replace('\'', '"');


        //when loading them by accident. Usual steps are:
        // 1.get the expected plain string (e.g nbCacheBytesMB)
        // 2.assign the string into the CoabVarsFileDto class as String
        // 3.serialize the string as YML and pass it into COAB vars file.
        // 4. vars file get interpolated potentially by credhub syntax, bosh, and possibly spruce templating.
        //
        // 5. eventually YML get loaded back from COAB into YML:
        //    - for fetching service instance values
        //    - for providing update last values

        ObjectMapper vulnerableJsonMapper = new ObjectMapper();
        //vulnerableJsonMapper.enableDefaultTyping();
        VulnerableBean vulnerableBean = vulnerableJsonMapper.readValue(JSON, VulnerableBean.class);

        //then
        //noinspection ThrowableNotThrown
        fail("Malicious class loaded from untrusted JSon! Details:" + vulnerableBean.obj.toString());
    }

    @Test
    public void create_dto_outputs_expected_vars_files_without_context_nor_params() throws JsonProcessingException {
        //Given
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        //when
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "service_id: \"service_definition_id\"\n" +
                        "plan_id: \"plan_guid\"\n" +
                        "context:\n" +
                        "  platform: \"cloudfoundry\"\n" +
                        "parameters: {}\n");
    }

    @Test
    public void update_dto_outputs_expected_vars_files() throws JsonProcessingException {
        //given
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";

        coabVarsFileDto.previous_values = new CoabVarsFileDto.PreviousValues();
        coabVarsFileDto.previous_values.plan_id = "previous_plan_guid";

        coabVarsFileDto.parameters.put("slowQuery", "false");
        coabVarsFileDto.parameters.put("cacheSizeMb", "10");

        //When
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
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

        String result = formatter.formatAsYml(aCreateServiceInstanceRequest());
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
