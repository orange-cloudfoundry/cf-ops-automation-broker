package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

public class YamlCataloglAsEnvironmentVarApplicationContextInitializerTest {

    @Before
    public void init() {
        System.setProperty("CATALOG_YML", CATALOG_YML);
        Assertions.assertThat(System.getProperty("CATALOG_YML")).isNotEmpty();
    }

    @After
    public void after() {
        System.clearProperty("CATALOG_YML");
        Assertions.assertThat(System.getProperty("CATALOG_YML")).isNull();
    }

    private final YamlCataloglAsEnvironmentVarApplicationContextInitializer contextInitializer = new YamlCataloglAsEnvironmentVarApplicationContextInitializer();

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
    public void initialize() throws Exception {
        StaticApplicationContext context = new StaticApplicationContext();

        contextInitializer.initialize(context);

        Assertions.assertThat(context.getEnvironment().getPropertySources().contains("catalog_from_env_var")).isTrue();
    }
}