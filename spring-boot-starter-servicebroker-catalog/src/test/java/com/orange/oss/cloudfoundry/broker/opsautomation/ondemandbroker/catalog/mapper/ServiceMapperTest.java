package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.ServiceProperties;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fest.assertions.MapAssert.entry;

public class ServiceMapperTest {

    public static final String SERVICE_ID = "a service id";
    public static final String SERVICE_DESCRIPTION = "a description";
    public static final String SERVICE_NAME = "a service name";

    @Test
    public void map_service_id() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getId()).as("toServiceDefinition serviceProperties id").isEqualTo(SERVICE_ID.toString());
    }

    @Test
    public void map_service_description() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setDescription(SERVICE_DESCRIPTION);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getDescription()).as("toServiceDefinition serviceProperties description").isEqualTo(SERVICE_DESCRIPTION);
    }

    @Test
    public void map_service_name() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setName(SERVICE_NAME);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getName()).as("toServiceDefinition serviceProperties name").isEqualTo(SERVICE_NAME);
    }

    @Test
    public void map_service_bindable() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setBindable(Boolean.FALSE);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.isBindable()).as("toServiceDefinition serviceProperties bindable").isFalse();
    }

    @Test
    public void map_service_requires() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setRequires(Arrays.asList("syslog_drain"));
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getRequires()).as("toServiceDefinition serviceProperties requires")
                .containsExactly("syslog_drain");
    }

    @Test
    public void map_service_plan_updateable() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setPlanUpdateable(Boolean.FALSE);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.isPlanUpdateable()).as("toServiceDefinition serviceProperties plan updateable").isFalse();
    }

    @Test
    public void map_service_metadata() {
        //GIVEN
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("displayName", "aDisplayName");
        metadata.put("longDescription", "a long description");
        metadata.put("providerDisplayName", "a provider");
        metadata.put("documentationUrl", "http://localhost/doc");
        metadata.put("imageUrl", "http://localhost/image.png");
        metadata.put("supportUrl", "http://localhost/support");

        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setMetadata(metadata);

        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getMetadata()).as("toServiceDefinition serviceProperties metadata").hasSize(6).includes(
                entry("displayName", "aDisplayName"),
                entry("longDescription", "a long description"),
                entry("providerDisplayName", "a provider"),
                entry("documentationUrl", "http://localhost/doc"),
                entry("imageUrl", "http://localhost/image.png"),
                entry("supportUrl", "http://localhost/support"));
    }

    @Test
    public void map_service_tags() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        serviceProperties.setTags(Stream.of("tag1", "tag2", "tag3").collect(Collectors.toList()));

        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getTags()).as("toServiceDefinition serviceProperties tags").containsOnly("tag1", "tag2", "tag3");
    }

    @Test
    public void does_not_map_requires() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getRequires()).as("does not toServiceDefinition requires").isNull();
    }

    @Test
    public void does_not_map_dashboard_client() {
        //GIVEN
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setId(SERVICE_ID);
        //WHEN
        final ServiceDefinition res = ServiceMapper.toServiceDefinition(serviceProperties);
        //THEN
        Assertions.assertThat(res.getDashboardClient()).as("does not toServiceDefinition dashboard client").isNull();
    }

    public void should_map_services() {
        //GIVEN
        final String aServiceId = "a service id";
        ServiceProperties aServiceProperties = new ServiceProperties();
        aServiceProperties.setId(aServiceId);
        final String anotherServiceId = "another service id";
        ServiceProperties anotherServiceProperties = new ServiceProperties();
        anotherServiceProperties.setId(anotherServiceId);
        //WHEN
        final List<ServiceDefinition> serviceDefinitions = ServiceMapper.toServiceDefinitions(Arrays.asList(aServiceProperties, anotherServiceProperties));
        //THEN
        Assertions.assertThat(serviceDefinitions).as("map services to service definitions").hasSize(2);
        Assertions.assertThat(serviceDefinitions.stream().map(ServiceDefinition::getId).collect(Collectors.toList())).as("map services to service definitions").containsOnly(aServiceId, anotherServiceId);


    }

}