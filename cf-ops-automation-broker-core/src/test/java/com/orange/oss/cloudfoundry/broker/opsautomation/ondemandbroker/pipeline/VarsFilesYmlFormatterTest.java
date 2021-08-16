package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class VarsFilesYmlFormatterTest {

    VarsFilesYmlFormatter formatter;

    private static Logger logger = LoggerFactory.getLogger(VarsFilesYmlFormatterTest.class.getName());

    @BeforeEach
    void setUp() {
        formatter = new VarsFilesYmlFormatter(false);
    }

    @Test
    public void handles_default_empty_osb_context_annotations_in_osb_cmdb() throws IOException {
        //cmdbJson_k8s_profile with special characters accepted
        assertParamJsonValueAccepted("{\n" +
            "        \"annotations\": {\n" +
            "          \"brokered_service_client_name\": \"osb-cmdb-backend-services-org-client-0\",\n" +
            "          \"brokered_service_context_organization_annotations\": \"{}\",\n" +
            "          \"brokered_service_context_spaceName\": \"smoke-tests\",\n" +
            "          \"brokered_service_context_space_annotations\": \"{}\",\n" +
            "          \"brokered_service_context_organizationName\": \"osb-cmdb-brokered-services-org-client-0\",\n" +
            "          \"brokered_service_context_instance_annotations\": \"{}\",\n" +
            "          \"brokered_service_api_info_location\": \"api.redacted-domain.org/v2/info\",\n" +
            "          \"brokered_service_context_instanceName\": \"gberche-osb-cmdb-0\"\n" +
            "        },\n" +
            "        \"labels\": {\n" +
            "          \"brokered_service_instance_guid\": \"0ff910ce-d1e8-437a-a40c-0a93cc27c9d7\",\n" +
            "          \"brokered_service_context_organization_guid\": \"c2169b61-9360-4d67-968c-575f3a10edf5\",\n" +
            "          \"brokered_service_originating_identity_user_id\": \"0fff310e-552c-4014-9943-d7acd9875865\",\n" +
            "          \"brokered_service_context_space_guid\": \"1a603476-a3a1-4c32-8021-d2a7b9b7c6b4\"\n" +
            "        }\n" +
            "      }");

        //white listed key with invalid character
        assertParamJsonValueRejected(
            "{\n" +
                "  \"brokered_service_originating_identity_groups\": {\n" +
                "    \"a-malicious-sub-key-with-variable\": \"$VARIABLE\" \n" +
                "  }\n" +
                "}");
    }

    @Test
    public void handles_whitelisted_osb_cmdb_patterns() throws IOException {
        //cmdbJson_k8s_profile with special characters accepted to support JSON encoded field
        assertParamJsonValueAccepted("{\n" +
            "  \"annotations\": {\n" +
            "    \"brokered_service_originating_identity_groups\": \"[\\\"system:serviceaccounts\\\",\\\"system:serviceaccounts:catalog\\\",\\\"system:authenticated\\\"]\",\n" +
            "    \"brokered_service_originating_identity_username\": \"system:serviceaccount:catalog:service-catalog-controller-manager\"\n" +
            "  },\n" +
            "  \"labels\": {\n" +
            "    \"brokered_service_context_clusterid\": \"ed99367d-2998-46c6-b700-59187141faf9\",\n" +
            "    \"brokered_service_instance_guid\": \"3c2def14-7073-4398-a901-bc4929b765aa\",\n" +
            "    \"brokered_service_originating_identity_uid\": \"d624ecee-99b0-4fd1-974f-0428bd9bc503\",\n" +
            "    \"brokered_service_context_instance_name\": \"poctv-mongodb-dedicated\",\n" +
            "    \"brokered_service_context_namespace\": \"fmt-private-mkaasq1\"\n" +
            "  }\n" +
            "}");

        //cmdbJson_cf_profile with JSON encoded annotations accepted in relaxed fields
        assertParamJsonValueAccepted("{\n" +
            "  \"annotations\": {\n" +
            "    \"brokered_service_context_organization_annotations\": \"{\\\"domain.com/org-key1\\\":\\\"org-value1\\\",\\\"orange.com/overrideable-key\\\":\\\"org-value2\\\"}\",\n" +
            "    \"brokered_service_context_space_annotations\": \"{\\\"domain.com/space-key1\\\":\\\"space-value1\\\",\\\"orange.com/overrideable-key\\\":\\\"space-value2\\\"}\",\n" +
            "    \"brokered_service_context_instance_annotations\": \"{\\\"domain.com/instance-key1\\\":\\\"instance-value1\\\",\\\"orange.com/overrideable-key\\\":\\\"instance-value2\\\"}\"\n" +
            "  },\n" +
            "  \"labels\": {\n" +
            "    \"brokered_service_context_clusterid\": \"ed99367d-2998-46c6-b700-59187141faf9\",\n" +
            "    \"brokered_service_instance_guid\": \"3c2def14-7073-4398-a901-bc4929b765aa\",\n" +
            "    \"brokered_service_originating_identity_uid\": \"d624ecee-99b0-4fd1-974f-0428bd9bc503\",\n" +
            "    \"brokered_service_context_instance_name\": \"poctv-mongodb-dedicated\",\n" +
            "    \"brokered_service_context_namespace\": \"fmt-private-mkaasq1\",\n" +
            "    \"brokered_service_context_orange_overrideable-key\": \"instance-value2\"" +
            "  }\n" +
            "}");

        //osb-cmdb v51 java.util.Map serialization format for Json Maps
        //accepted/in relaxed fields
        assertParamJsonValueAccepted("{\n" +
            "  \"annotations\": {\n" +
            "    \"brokered_service_context_organization_annotations\": \"{orange.com/orangecarto=26582}\",\n" +
            "  }\n" +
            "}");

        //white listed key with invalid character
        assertParamJsonValueRejected(
            "{\n" +
                "  \"brokered_service_originating_identity_groups\": {\n" +
                "    \"a-malicious-sub-key-with-variable\": \"$VARIABLE\" \n" +
                "  }\n" +
                "}");
    }

    @Test
    void accepts_maintenance_info_field_into_coab_vars() throws IOException {
        CoabVarsFileDto coabVarsFileDtoWithoutMaintenanceInfo = aTypicalUserProvisionningRequest();
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDtoWithoutMaintenanceInfo);

        CoabVarsFileDto coabVarsFileDtoWithMaintenanceInfo = aTypicalUserProvisionningRequest();
        coabVarsFileDtoWithMaintenanceInfo.maintenanceInfo = OsbBuilderHelper.anInitialMaintenanceInfo();
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDtoWithMaintenanceInfo);
    }

    @Test
    void accepts_previous_value_field_into_coab_vars() throws IOException {
        CoabVarsFileDto coabVarsFileDtoWithoutValue = aTypicalUserProvisionningRequest();
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDtoWithoutValue);

        CoabVarsFileDto coabVarsFileDtoWithPreviousValue = aTypicalUserProvisionningRequest();
        coabVarsFileDtoWithPreviousValue.previous_values = new CoabVarsFileDto.PreviousValues();
        coabVarsFileDtoWithPreviousValue.previous_values.plan_id="older_plan";
        coabVarsFileDtoWithPreviousValue.previous_values.maintenanceInfo=OsbBuilderHelper.anInitialMaintenanceInfo();
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDtoWithPreviousValue);

        CoabVarsFileDto coabVarsFileDtoWithPreviousValueWithNoPlan = aTypicalUserProvisionningRequest();
        coabVarsFileDtoWithPreviousValueWithNoPlan.previous_values = new CoabVarsFileDto.PreviousValues();
        coabVarsFileDtoWithPreviousValueWithNoPlan.previous_values.plan_id=null;
        coabVarsFileDtoWithPreviousValueWithNoPlan.previous_values.maintenanceInfo=OsbBuilderHelper.anInitialMaintenanceInfo();
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDtoWithPreviousValueWithNoPlan);
    }

    @Test
    public void accepts_invalid_patterns_when_input_validation_disabled() throws IOException {
        //given a formatter with input validation disabled
        formatter = new VarsFilesYmlFormatter(true);

        //when invalid input is received
        //then it is accepted
        assertParamValueAccepted("$(a_malicious_shell_command_to_inject_in_bosh_templates)");
    }

    @Test
    public void accepts_valid_patterns() throws IOException {
        assertParamValueAccepted("a string with spaces");
        assertParamValueAccepted("10mb");
        assertParamValueAccepted("a deployment model cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        assertParamValueAccepted("org_guid1");
        assertParamValueAccepted("c_5f89138b-ed9a-4596-a042-a6349b6a1f04 ");
        assertParamValueAccepted("cacheSizeMb");
        assertParamValueAccepted("10");
        assertParamValueAccepted("10.3");

        //cmdbJson_cf_profile without special characters
        assertParamJsonValueAccepted("{\n" +
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
            "  }\n");
    }
    @Test
    public void rejects_invalid_patterns() throws IOException {
        assertInputRejectedFromFieldsUsedByCoabModels("`a_malicious_shell_command_to_inject_in_bosh_templates`");
        assertInputRejectedFromFieldsUsedByCoabModels("``");
        assertInputRejectedFromFieldsUsedByCoabModels("$(a_malicious_shell_command_to_inject_in_bosh_templates)");
        assertInputRejectedFromFieldsUsedByCoabModels("$()");
        assertInputRejectedFromFieldsUsedByCoabModels("(("); //credhub interpolation
        assertInputRejectedFromFieldsUsedByCoabModels("(( a/string ))");
        assertInputRejectedFromFieldsUsedByCoabModels("))");
        assertInputRejectedFromFieldsUsedByCoabModels("?"); //other unknown chars
        assertInputRejectedFromFieldsUsedByCoabModels("&"); //YML references
        assertInputRejectedFromFieldsUsedByCoabModels("\n"); //YML new lines
        assertInputRejectedFromFieldsUsedByCoabModels("\r"); //YML new lines
        //<script>alert('XSS')</script>
        assertInputRejectedFromFieldsUsedByCoabModels("<");
        assertInputRejectedFromFieldsUsedByCoabModels("/");
        assertInputRejectedFromFieldsUsedByCoabModels("a/path");
        assertInputRejectedFromFieldsUsedByCoabModels("../../etc/password");
        assertInputRejectedFromFieldsUsedByCoabModels("api.mycf.org/v2/info"); //this is now in json encoded fields
        // not used by coab
        assertInputRejectedFromFieldsUsedByCoabModels(">");
        assertInputRejectedFromFieldsUsedByCoabModels("'");
        assertInputRejectedFromFieldsUsedByCoabModels("(");
        assertInputRejectedFromFieldsUsedByCoabModels(")");
        assertTooLongContentIsRejected(
            RandomStringUtils.randomAlphabetic(VarsFilesYmlFormatter.MAX_SERIALIZED_SIZE + 1));


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
            //noinspection ResultOfMethodCallIgnored
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
            //noinspection ResultOfMethodCallIgnored
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

    protected void assertInputRejectedFromFieldsUsedByCoabModels(String paramValue) throws JsonProcessingException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.deployment_name = paramValue;
        Map<String, Object> osbCmdbMap = new HashMap<>();
        osbCmdbMap.put("brokered_service_instance_guid", paramValue);
        osbCmdbMap.put("brokered_service_context_orange_app_code", paramValue);
        coabVarsFileDto.parameters.put("x-osb-cmdb",osbCmdbMap);
        coabVarsFileDto.parameters.put("aparam", paramValue);
        coabVarsFileDto.parameters.put("a_nested_param", Collections.singletonMap("a_key", paramValue));
        //noinspection EmptyCatchBlock
        try {
            formatter.formatAsYml(coabVarsFileDto);
            //noinspection ResultOfMethodCallIgnored
            fail("expected " + paramValue + "to be rejected");
        } catch (UserFacingRuntimeException e) {
            assertThat(e.getMessage()).contains("deployment_name");
            assertThat(e.getMessage()).contains("brokered_service_instance_guid");
            assertThat(e.getMessage()).contains("brokered_service_context_orange_app_code");
            assertThat(e.getMessage()).contains("aparam");
            assertThat(e.getMessage()).contains("a_key");
        }
    }

    protected void assertParamJsonValueRejected(String jsonParamValueToDeserialize) throws IOException {
        //noinspection rawtypes
        Map deserializedJson = (Map) parseFromYml(jsonParamValueToDeserialize, Map.class);
        assertParamValueRejected(deserializedJson);
    }

    protected void assertParamValueRejected(Object paramValue) throws JsonProcessingException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.deployment_name = "aName";
        coabVarsFileDto.parameters.put("aparam", paramValue);
        //noinspection EmptyCatchBlock
        try {
            formatter.formatAsYml(coabVarsFileDto);
            //noinspection ResultOfMethodCallIgnored
            fail("expected " + paramValue + "to be rejected");
        } catch (UserFacingRuntimeException e) {
            assertThat(e.getMessage()).contains("aparam");
        }
    }

    protected void assertParamJsonValueAccepted(String jsonParamValueToDeserialize) throws IOException {
        //noinspection rawtypes
        Map deserializedJsonParam = (Map) parseFromYml(jsonParamValueToDeserialize, Map.class);
        assertParamValueAccepted(deserializedJsonParam);
    }


    protected void assertParamValueAccepted(Object paramValue) throws IOException {
        CoabVarsFileDto coabVarsFileDto = aTypicalUserProvisionningRequest();
        coabVarsFileDto.parameters.put("aparam", paramValue);
        //assert value is accepted
        assertDtoSerializesAndDeserializesWithoutError(coabVarsFileDto);
    }

    private void assertDtoSerializesAndDeserializesWithoutError(CoabVarsFileDto coabVarsFileDto) throws IOException {
        String yamlDump = formatter.formatAsYml(coabVarsFileDto);

        //assert it can be parsed back into Dto
        CoabVarsFileDto deserialized = (CoabVarsFileDto) parseFromYml(yamlDump, coabVarsFileDto.getClass());
        logger.debug("deserialized into {} from yaml {}", deserialized, yamlDump);
        assertThat(coabVarsFileDto).isEqualTo(deserialized);
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

        //and parse back from yml. This is necessary when tracking deployment completion
        @SuppressWarnings("unused") CoabVarsFileDto deserialized = formatter.parseFromYml(result);
        //potentially check that both are identical (need to debug reflection equals)
        assertThat(coabVarsFileDto).isEqualTo(deserialized);
    }

    protected Object parseFromYml(String result, @SuppressWarnings("rawtypes") Class expectedClass) throws IOException {
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
