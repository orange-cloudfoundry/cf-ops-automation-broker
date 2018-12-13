package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;


public class JacksonConfigTest {

    OsbClientFeignConfig osbClientFeignConfig = new OsbClientFeignConfig();
    ObjectMapper objectMapper = new ObjectMapper();

    String openBrokerReferenceJson = "{\n" +
            "  \"services\": [\n" +
            "    {\n" +
            "      \"id\": \"service-one-id\",\n" +
            "      \"name\": \"Service One\",\n" +
            "      \"description\": \"Description for Service One\",\n" +
            "      \"bindable\": true,\n" +
            "      \"dashboard_client\": {\n" +
            "        \"id\": \"dash-id\",\n" +
            "        \"secret\": \"dash-secret\",\n" +
            "        \"redirect_uri\": \"https://somewhere.local\"\n" +
            "      },\n" +
            "      \"requires\": [\n" +
            "        \"syslog_drain\",\n" +
            "        \"route_forwarding\"\n" +
            "      ],\n" +
            "      \"plans\": [\n" +
            "        {\n" +
            "          \"id\": \"plan-one-id\",\n" +
            "          \"name\": \"Plan One\",\n" +
            "          \"description\": \"Description for Plan One\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": \"plan-two-id\",\n" +
            "          \"name\": \"Plan Two\",\n" +
            "          \"description\": \"Description for Plan Two\",\n" +
            "          \"metadata\": {\n" +
            "            \"key1\": \"value1\",\n" +
            "            \"key2\": \"value2\"\n" +
            "          },\n" +
            "          \"bindable\": false,\n" +
            "          \"free\": true,\n" +
            "          \"schemas\": {\n" +
            "            \"service_instance\": {\n" +
            "              \"create\": {\n" +
            "                \"parameters\": {\n" +
            "                  \"$schema\": \"http://example.com/service/create/schema\",\n" +
            "                  \"type\": \"object\"\n" +
            "                }\n" +
            "              },\n" +
            "              \"update\": {\n" +
            "                \"parameters\": {\n" +
            "                  \"$schema\": \"http://example.com/service/update/schema\",\n" +
            "                  \"type\": \"object\"\n" +
            "                }\n" +
            "              }\n" +
            "            },\n" +
            "            \"service_binding\": {\n" +
            "              \"create\": {\n" +
            "                \"parameters\": {\n" +
            "                  \"$schema\": \"http://example.com/binding/create/schema\",\n" +
            "                  \"type\": \"object\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    public void native_spring_deserialization() throws IOException {
        Catalog deserializedCatalog = objectMapper.readValue(openBrokerReferenceJson, Catalog.class);
        assertThat(deserializedCatalog).isNotNull();
    }

    @Test
    public void deserializes_CatalogResponse() throws IOException {
        //given a json string to deserialize
        Plan plan  = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan2 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        Plan plan3 = Plan.builder().id("plan_id3").name("plan_name3").description("plan_description3").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan, plan2)).build();

        ServiceDefinition serviceDefinition2 = ServiceDefinition.builder()
                .id("service_id2")
                .name("service_name2")
                .description("service_description3")
                .bindable(true)
                .plans(Collections.singletonList(plan3)).build();
        Catalog catalog = Catalog.builder().serviceDefinitions(serviceDefinition
        ).build();
        StringWriter jsonWritter = new StringWriter();
        objectMapper.writerFor(Catalog.class).writeValue(jsonWritter, catalog);
        String json = jsonWritter.toString();

        //Alternative is to manually extract Json using debugger with expression
        // IOUtils.toString(inputMessage.getBody(), defaultCharset);
        //at stack
        // readJavaType:237, AbstractJackson2HttpMessageConverter (org.springframework.http.converter.json)
        //read:225, AbstractJackson2HttpMessageConverter (org.springframework.http.converter.json)
        //extractData:100, HttpMessageConverterExtractor (org.springframework.web.client)
        //decode:60, SpringDecoder (org.springframework.cloud.openfeign.support)
        //decode:45, ResponseEntityDecoder (org.springframework.cloud.openfeign.support)
        //decode:36, OptionalDecoder (feign.optionals)
        //decode:170, SynchronousMethodHandler (feign)
        //executeAndDecode:134, SynchronousMethodHandler (feign)
        //invoke:77, SynchronousMethodHandler (feign)
        //invoke:102, ReflectiveFeign$FeignInvocationHandler (feign)
        //getCatalog:-1, $Proxy124 (com.sun.proxy)
        //feign_client_unmarshalls_bind_responses:93, OsbClientTestApplicationTest (com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient)

        //then
        Catalog deserializedCatalog = objectMapper.readValue(json, Catalog.class);
        assertThat(deserializedCatalog).isNotNull();
        assertThat(deserializedCatalog).isEqualTo(catalog);
    }

    @Test
    public void deserializes_Response() throws IOException {
        //given a json string to deserialize
        CreateServiceInstanceResponse initialObject = CreateServiceInstanceResponse.builder()
                .async(true)
                .dashboardUrl("https://a-inner-dashboard.com").build();

        StringWriter jsonWritter = new StringWriter();

        objectMapper.writerFor(CreateServiceInstanceResponse.class).writeValue(jsonWritter, initialObject);
        String json = jsonWritter.toString();
        System.out.println("json = " + json);


        //then
        CreateServiceInstanceResponse deserialized = objectMapper.readValue(json, CreateServiceInstanceResponse.class);
        assertThat(deserialized).isEqualTo(initialObject);
    }

    @Test
    public void deserializes_parent_fields_from_CreateServiceInstanceResponse() throws IOException {
        CreateServiceInstanceRequest createServiceInstanceRequest = OsbBuilderHelper.aCreateServiceInstanceRequest();
        StringWriter jsonWritter = new StringWriter();
        objectMapper.writerFor(CreateServiceInstanceRequest.class).writeValue(jsonWritter, createServiceInstanceRequest);
        String json = jsonWritter.toString();
        System.out.println("json = " + json);
        //Manually extracted using debugger with expression
        // IOUtils.toString(inputMessage.getBody(), defaultCharset);
        //at stack
        // readJavaType:237, AbstractJackson2HttpMessageConverter (org.springframework.http.converter.json)
        //read:225, AbstractJackson2HttpMessageConverter (org.springframework.http.converter.json)
        //extractData:100, HttpMessageConverterExtractor (org.springframework.web.client)
        //decode:60, SpringDecoder (org.springframework.cloud.openfeign.support)
        //decode:45, ResponseEntityDecoder (org.springframework.cloud.openfeign.support)
        //decode:36, OptionalDecoder (feign.optionals)
        //decode:170, SynchronousMethodHandler (feign)
        //executeAndDecode:134, SynchronousMethodHandler (feign)
        //invoke:77, SynchronousMethodHandler (feign)
        //invoke:102, ReflectiveFeign$FeignInvocationHandler (feign)
        //getCatalog:-1, $Proxy124 (com.sun.proxy)
        //feign_client_unmarshalls_bind_responses:93, OsbClientTestApplicationTest (com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient)
        //invoke0:-1, NativeMethodAccessorImpl (sun.reflect)
        //invoke:62, NativeMethodAccessorImpl (sun.reflect)
        //invoke:43, DelegatingMethodAccessorImpl (sun.reflect)
        //invoke:498, Method (java.lang.reflect)
        //runReflectiveCall:50, FrameworkMethod$1 (org.junit.runners.model)
        //run:12, ReflectiveCallable (org.junit.internal.runners.model)
        //invokeExplosively:47, FrameworkMethod (org.junit.runners.model)
        //evaluate:17, InvokeMethod (org.junit.internal.runners.statements)
        //evaluate:73, RunBeforeTestExecutionCallbacks (org.springframework.test.context.junit4.statements)
        //evaluate:83, RunAfterTestExecutionCallbacks (org.springframework.test.context.junit4.statements)
        //evaluate:75, RunBeforeTestMethodCallbacks (org.springframework.test.context.junit4.statements)
        //evaluate:86, RunAfterTestMethodCallbacks (org.springframework.test.context.junit4.statements)
        //evaluate:239, ExpectedException$ExpectedExceptionStatement (org.junit.rules)
        //evaluate:73, WireMockRule$1 (com.github.tomakehurst.wiremock.junit)
        //evaluate:20, RunRules (org.junit.rules)
        //evaluate:84, SpringRepeat (org.springframework.test.context.junit4.statements)
        //runLeaf:325, ParentRunner (org.junit.runners)
        //runChild:251, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
        //runChild:97, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
        //run:290, ParentRunner$3 (org.junit.runners)
        //schedule:71, ParentRunner$1 (org.junit.runners)
        //runChildren:288, ParentRunner (org.junit.runners)
        //access$000:58, ParentRunner (org.junit.runners)
        //evaluate:268, ParentRunner$2 (org.junit.runners)
        //evaluate:61, RunBeforeTestClassCallbacks (org.springframework.test.context.junit4.statements)
        //evaluate:70, RunAfterTestClassCallbacks (org.springframework.test.context.junit4.statements)
        //run:363, ParentRunner (org.junit.runners)
        //run:190, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
        //run:137, JUnitCore (org.junit.runner)
        //startRunnerWithArgs:68, JUnit4IdeaTestRunner (com.intellij.junit4)
        //startRunnerWithArgs:47, IdeaTestRunner$Repeater (com.intellij.rt.execution.junit)
        //prepareStreamsAndStart:242, JUnitStarter (com.intellij.rt.execution.junit)
        //main:70, JUnitStarter (com.intellij.rt.execution.junit)
        json = "{\"services\":[{\"id\":\"ondemand-service\",\"name\":\"ondemand\",\"description\":\"A simple ondemand service broker implementation\",\"bindable\":true,\"plan_updateable\":false,\"plans\":[{\"id\":\"ondemand-plan\",\"name\":\"default\",\"description\":\"This is a default ondemand plan.  All services are created equally.\",\"metadata\":{\"costs\":[{\"amount\":{\"usd\":0.0},\"unit\":\"MONTHLY\"}],\"bullets\":[\"Dedicated ondemand server\",\"100 MB Storage (not enforced)\",\"40 concurrent connections (not enforced)\"]},\"free\":true}],\"tags\":[\"ondemand\",\"document\"],\"metadata\":{\"longDescription\":\"ondemand Service\",\"documentationUrl\":\"https://orange.com\",\"providerDisplayName\":\"Orange\",\"displayName\":\"ondemand\",\"imageUrl\":\"http://info.mongodb.com/rs/mongodb/images/MongoDB_Logo_Full.png\",\"supportUrl\":\"https://orange.com\"},\"requires\":[],\"dashboard_client\":null}]}";

//        String json = "{\"operation\":\"{\\\"org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest\\\":{\\\"serviceDefinitionId\\\":\\\"cassandra-ondemand-service\\\",\\\"planId\\\":\\\"cassandra-ondemand-plan\\\",\\\"organizationGuid\\\":\\\"org_id\\\",\\\"spaceGuid\\\":\\\"space_id\\\",\\\"serviceInstanceId\\\":\\\"111\\\",\\\"serviceDefinition\\\":{\\\"id\\\":\\\"cassandra-ondemand-service\\\",\\\"name\\\":\\\"cassandra-ondemand\\\",\\\"description\\\":\\\"On demand cassandra dedicated clusters\\\",\\\"bindable\\\":true,\\\"planUpdateable\\\":true,\\\"plans\\\":[{\\\"id\\\":\\\"cassandra-ondemand-plan\\\",\\\"name\\\":\\\"default\\\",\\\"description\\\":\\\"This is a default ondemand plan.  All services are created equally.\\\",\\\"metadata\\\":{},\\\"free\\\":true}],\\\"tags\\\":[\\\"ondemand\\\",\\\"document\\\"],\\\"metadata\\\":{\\\"displayName\\\":\\\"ondemand\\\",\\\"imageUrl\\\":\\\"https://orange.com/image.png\\\",\\\"longDescription\\\":\\\"A dedicated on-demand cassandra cluster\\\",\\\"providerDisplayName\\\":\\\"Orange\\\",\\\"documentationUrl\\\":\\\"https://orange.com/doc\\\",\\\"supportUrl\\\":\\\"https://orange.com/support\\\"}},\\\"parameters\\\":{},\\\"context\\\":{\\\"platform\\\":\\\"cloudfoundry\\\",\\\"properties\\\":{}},\\\"asyncAccepted\\\":true,\\\"apiInfoLocation\\\":\\\"api-info\\\",\\\"originatingIdentity\\\":{\\\"platform\\\":\\\"cloudfoundry\\\",\\\"properties\\\":{\\\"user_id\\\":\\\"user_guid\\\",\\\"email\\\":\\\"user_email\\\"}}},\\\"startRequestDate\\\":\\\"2018-06-07T16:55:48.121Z\\\"}\"}";
        CreateServiceInstanceResponse createServiceInstanceResponse = objectMapper.readValue(json, CreateServiceInstanceResponse.class);
        assertThat(createServiceInstanceResponse.getOperation()).contains("CreateServiceInstanceRequest");
    }

}