package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.fest.assertions.Assertions;
import org.junit.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CatalogYamlPropertySourceMapperTest {

    public static final String CATALOG_YML = "servicebroker:\n" +
            "  catalog:\n" +
            "    services:\n" +
            "    - id: ondemand-service\n" +
            "      name: ondemand\n" +
            "      description: try a simple ondemand service broker implementation\n" +
            "      bindable: true\n" +
            "      plans:\n" +
            "        - id: ondemand-plan\n" +
            "          name: default\n" +
            "          description: This is a default ondemand plan.  All services are created equally.\n" +
            "      tags:\n" +
            "        -ondemand\n" +
            "        -document\n" +
            "      metadata:\n" +
            "        displayName: ondemand\n" +
            "        imageUrl: https://orange.com/image.png\n" +
            "        longDescription: ondemand Service\n" +
            "        providerDisplayName: Orange\n" +
            "        documentationUrl: https://orange.com/doc\n" +
            "        supportUrl: https://orange.com/support\n" +
            "\n" +
            "\n";

    @Test
    public void should_map_a_yml_catalog_into_property_source() throws Exception {
        Resource resource = new InMemoryResource(CATALOG_YML);

        PropertySource<?> propertySource = CatalogYamlPropertySourceMapper.toPropertySource(resource);

        //very basic assertions
        Assertions.assertThat(propertySource.getProperty("servicebroker.catalog.services[0].id")).isEqualTo("ondemand-service");
        Assertions.assertThat(propertySource.getProperty("servicebroker.catalog.services[0].plans[0].id")).isEqualTo("ondemand-plan");
    }

    @Test
    public void fail_to_map_invalid_yml_catalog_into_property_source() throws Exception {
        //invalid yml syntax
        String catalogYml = "servicebroker: catalog:\n";
        Resource resource = new InMemoryResource(catalogYml);

        assertThatThrownBy(() -> {
            CatalogYamlPropertySourceMapper.toPropertySource(resource);
        }).hasMessageContaining("mapping values are not allowed here");
    }
}