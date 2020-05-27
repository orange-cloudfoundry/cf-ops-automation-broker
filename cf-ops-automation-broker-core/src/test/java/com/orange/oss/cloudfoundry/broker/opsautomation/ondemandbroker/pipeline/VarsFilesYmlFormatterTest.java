package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class VarsFilesYmlFormatterTest {

    VarsFilesYmlFormatter formatter = new VarsFilesYmlFormatter();

    private static Logger logger = LoggerFactory.getLogger(VarsFilesYmlFormatterTest.class.getName());

    @Test
    public void rejects_invalid_patterns() throws JsonProcessingException {
        assertDeploymentNameAndParamValueRejected("`a_malicious_shell_command_to_inject_in_bosh_templates`");
        assertDeploymentNameAndParamValueRejected("``");
        assertDeploymentNameAndParamValueRejected("$(a_malicious_shell_command_to_inject_in_bosh_templates)");
        assertDeploymentNameAndParamValueRejected("$()");
        assertDeploymentNameAndParamValueRejected("(("); //credhub interpolation
        assertDeploymentNameAndParamValueRejected("(( a/string ))");
        assertDeploymentNameAndParamValueRejected("))");
        assertDeploymentNameAndParamValueRejected("?"); //other unknown chars
        assertDeploymentNameAndParamValueRejected("&"); //YML references
        assertTooLongContentIsRejected(
            RandomStringUtils.randomAlphabetic(VarsFilesYmlFormatter.MAX_SERIALIZED_SIZE + 1));


        assertParamValueAccepted("a/path");
        assertParamValueAccepted("../../etc/password");
        assertParamValueAccepted("api.mycf.org/v2/info");
        assertParamValueAccepted("a string with spaces");
        assertParamValueAccepted("10mb");
        assertParamValueAccepted("a deployment model cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        assertParamValueAccepted("org_guid1");
        assertParamValueAccepted("c_5f89138b-ed9a-4596-a042-a6349b6a1f04 ");
        assertParamValueAccepted("cacheSizeMb");
        assertParamValueAccepted("10");
        assertParamValueAccepted("10.3");
        String cmdbJson = "{\n" +
            "    \"labels\": {\n" +
            "      \"brokered_service_instance_guid\": \"3aa96c94-1d01-4389-ab4f-260d99257215\",\n" +
            "      \"brokered_service_context_organization_guid\": \"c2169b61-9360-4d67-968c-575f3a10edf5\",\n" +
            "      \"brokered_service_originating_identity_user_id\": \"0d02117b-aa21-43e2-b35e-8ad6f8223519\",\n" +
            "      \"brokered_service_context_space_guid\": \"1a603476-a3a1-4c32-8021-d2a7b9b7c6b4\",\n" +
            "      \"backing_service_instance_guid\": \"191260bb-3477-422d-8f40-bf053ccf6930\"\n" +
            "    },\n" +
            "    \"annotations\": {\n" +
            "      \"brokered_service_context_instance_name\": \"osb-cmdb-broker-0-smoketest-1578565892\",\n" +
            "      \"brokered_service_context_space_name\": \"smoke-tests\",\n" +
            "      \"brokered_service_api_info_location\": \"api.mycf.org/v2/info\",\n" +
            "      \"brokered_service_context_organization_name\": \"osb-cmdb-brokered-services-org-client-0\"\n" +
            "    }\n" +
            "  }\n";
        Map osbCmdbParamValue = new ObjectMapper().readValue(cmdbJson, Map.class);
        assertParamValueAccepted(osbCmdbParamValue);

    }

    private void assertTooLongContentIsRejected(String aTooLongString) throws JsonProcessingException {
        //YML references
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.deployment_name = "a-name";
        coabVarsFileDto.parameters.put("aparam",
            aTooLongString);
        //noinspection EmptyCatchBlock
        try {
            formatter.formatAsYml(coabVarsFileDto);
            fail("expected " + "&" + "to be rejected");
        } catch (UserFacingRuntimeException e) {
            assertThat(e.getMessage()).contains("too long");
        }
    }

    @Test
    public void rejects_invalid_patterns_with_precise_message() throws JsonProcessingException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.deployment_name = "((";
        coabVarsFileDto.context.organization_guid = "((";
        coabVarsFileDto.context.platform = "((";
        coabVarsFileDto.context.space_guid = "((";
        coabVarsFileDto.context.user_guid = "((";
        coabVarsFileDto.plan_id = "((";
        coabVarsFileDto.service_id = "((";
        coabVarsFileDto.previous_values = new CoabVarsFileDto.PreviousValues();
        coabVarsFileDto.previous_values.plan_id = "((";
        coabVarsFileDto.parameters.put("aparam", "((");
        coabVarsFileDto.parameters.put("param-with-chars-((", "valid-value");
        coabVarsFileDto.parameters.put("a-param-with-unexpected-value", new ArrayList<>());
        try {
            formatter.formatAsYml(coabVarsFileDto);
            fail("expected " + "((" + "to be rejected");
        } catch (UserFacingRuntimeException e) {
            String patternMessage = CoabVarsFileDto.WHITE_LISTED_MESSAGE;
            assertThat(e.getMessage()).contains("deployment_name " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("context.platform " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("context.organization_guid " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("context.space_guid " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("context.user_guid " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("plan_id " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("service_id " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("previous_values.plan_id " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("parameter aparam " + patternMessage + " whereas it has content:((");
            assertThat(e.getMessage()).contains("parameter name param-with-chars-(( " + patternMessage);
            assertThat(e.getMessage()).contains("a-param-with-unexpected-value of unsupported type: java.util.ArrayList");
        }
    }

    protected void assertDeploymentNameAndParamValueRejected(String paramValue) throws JsonProcessingException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.deployment_name = paramValue;
        coabVarsFileDto.parameters.put("aparam", paramValue);
        //noinspection EmptyCatchBlock
        try {
            formatter.formatAsYml(coabVarsFileDto);
            fail("expected " + paramValue + "to be rejected");
        } catch (UserFacingRuntimeException e) {
            assertThat(e.getMessage()).contains("deployment_name");
            assertThat(e.getMessage()).contains("aparam");
        }
    }

    protected void assertParamValueAccepted(Object paramValue) throws JsonProcessingException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.parameters.put("aparam", paramValue);
        formatter.formatAsYml(coabVarsFileDto);
    }


    protected CoabVarsFileDto aTypicalUserProvisionningRequest() {
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.instance_id = "service_instance_id";
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
        coabVarsFileDto.instance_id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";


        coabVarsFileDto.parameters.put("slowQuery", false);
        coabVarsFileDto.parameters.put("cacheSizeMb", 10);
        coabVarsFileDto.parameters.put("apiKey", "A safe STRING");

        //when
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "instance_id: \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "service_id: \"service_definition_id\"\n" +
                        "plan_id: \"plan_guid\"\n" +
                        "context:\n" +
                        "  platform: \"cloudfoundry\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "  organization_guid: \"org_guid1\"\n" +
                        "parameters:\n" +
                        "  apiKey: \"A safe STRING\"\n" +
                        "  cacheSizeMb: 10\n" +
                        "  slowQuery: false\n");

        //and potentially in the future parse back from yml, e.g. for OSB get endpoints
        @SuppressWarnings("unused") Object deserialized = parseFromYml(result, coabVarsFileDto.getClass());
        //potentially check that both are identitical (need to debug reflection equals)
        //        assertThat(reflectionEquals(coabVarsFileDto, deserialized)).isTrue();
    }

    protected Object parseFromYml(String result, Class expectedClass) throws IOException {
        //noinspection unchecked
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
    @Disabled
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
        //noinspection ResultOfMethodCallIgnored
        fail("Malicious class loaded from untrusted JSon! Details:" + vulnerableBean.obj.toString());
    }

    @Test
    public void create_dto_outputs_expected_vars_files_without_context_nor_params() throws JsonProcessingException {
        //Given
        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
        coabVarsFileDto.instance_id = "service_instance_id";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        //when
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "instance_id: \"service_instance_id\"\n" +
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
        coabVarsFileDto.instance_id = "service_instance_id";
        coabVarsFileDto.service_id = "service_definition_id";
        coabVarsFileDto.plan_id = "plan_guid";

        coabVarsFileDto.context.user_guid = "user_guid1";
        coabVarsFileDto.context.space_guid = "space_guid1";
        coabVarsFileDto.context.organization_guid = "org_guid1";

        coabVarsFileDto.previous_values = new CoabVarsFileDto.PreviousValues();
        coabVarsFileDto.previous_values.plan_id = "previous_plan_guid";

        coabVarsFileDto.parameters.put("slowQuery", false);
        coabVarsFileDto.parameters.put("cacheSizeMb", 10);
        coabVarsFileDto.parameters.put("cacheRatio", 0.8642);

        //When
        String result = formatter.formatAsYml(coabVarsFileDto);
        logger.info("vars.yml serialized yml content:\n{}", result);

        //then
        assertThat(result).isEqualTo(
                "---\n" +
                        "deployment_name: \"cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0\"\n" +
                        "instance_id: \"service_instance_id\"\n" +
                        "service_id: \"service_definition_id\"\n" +
                        "plan_id: \"plan_guid\"\n" +
                        "context:\n" +
                        "  platform: \"cloudfoundry\"\n" +
                        "  user_guid: \"user_guid1\"\n" +
                        "  space_guid: \"space_guid1\"\n" +
                        "  organization_guid: \"org_guid1\"\n" +
                        "parameters:\n" +
                        "  cacheRatio: 0.8642\n" +
                        "  cacheSizeMb: 10\n" +
                        "  slowQuery: false\n" +
                        "previous_values:\n" +
                        "  plan_id: \"previous_plan_guid\"\n");
    }

    private static final String SERVICE_DEFINITION_ID = "cassandra-ondemand-service";
    private static final String SERVICE_PLAN_ID = "cassandra-ondemand-plan";
    private static final String SERVICE_INSTANCE_ID = "111";
    private static final String SERVICE_BINDING_INSTANCE_ID = "222";


    public static CreateServiceInstanceRequest aCreateServiceInstanceRequest(){

        return CreateServiceInstanceRequest.builder()
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .planId(SERVICE_PLAN_ID)
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id")
                        .spaceGuid("space_id")
                        .build()
                )
                .build();
    }

    public static org.springframework.cloud.servicebroker.model.Context aCfOsbContext() {
        return CloudFoundryContext.builder()
                .organizationGuid("org_id")
                .spaceGuid("space_id")
                .build();
    }



}
