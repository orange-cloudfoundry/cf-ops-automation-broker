package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlCataloglAsEnvironmentVarApplicationContextInitializerTest {

    @Before
    public void init() {
        System.setProperty("CATALOG_YML", CATALOG_YML);
        assertThat(System.getProperty("CATALOG_YML")).isNotEmpty();
    }

    @After
    public void after() {
        System.clearProperty("CATALOG_YML");
        assertThat(System.getProperty("CATALOG_YML")).isNull();
    }

    private final YamlCataloglAsEnvironmentVarApplicationContextInitializer contextInitializer = new YamlCataloglAsEnvironmentVarApplicationContextInitializer();

    public static final String CATALOG_YML = "spring:\n" +
            "  cloud:\n" +
            "    openservicebroker:\n" +
            "      catalog:\n" +
            "        services:\n" +
            "          - id: ondemand-service\n" +
            "            name: ondemand\n" +
            "            description: try a simple ondemand service broker implementation\n" +
            "            bindable: true\n" +
            "            plans:\n" +
            "              - id: ondemand-plan\n" +
            "                name: default\n" +
            "                description: This is a default ondemand plan.  All services are created equally.\n" +
            "            tags:\n" +
            "              -ondemand\n" +
            "              -document\n" +
            "            metadata:\n" +
            "              displayName: ondemand\n" +
            "              imageUrl: https://orange.com/image.png\n" +
            "              longDescription: ondemand Service\n" +
            "              providerDisplayName: Orange\n" +
            "              documentationUrl: https://orange.com/doc\n" +
            "              supportUrl: https://orange.com/support\n";

    @Test
    public void loads_yml_env_var_as_property_source_into_spring_context() throws Exception {
        StaticApplicationContext context = new StaticApplicationContext();

        contextInitializer.initialize(context);

        assertThat(context.getEnvironment().getPropertySources().contains("catalog_from_env_var")).isTrue();
    }


    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceBrokerAutoConfiguration.class))
            .withInitializer(new YamlCataloglAsEnvironmentVarApplicationContextInitializer());


    /**
     * Declares a dummy service instance service, which is necessary for the osb lib autoconfiguration to trigger
     * and fetch the catalog
     */
    @Configuration
    public static class NoCatalogBeanConfiguration {
        @Bean
        public ServiceInstanceService serviceInstanceService() {
            return new TestServiceInstanceService();
        }

        public static class TestServiceInstanceService implements ServiceInstanceService {
            @Override
            public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
                return null;
            }

            @Override
            public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
                return null;
            }

            @Override
            public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
                return null;
            }

            @Override
            public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
                return null;
            }
        }
    }


    /**
     * Inspired from org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfigurationTest#servicesAreCreatedFromCatalogProperties
     * See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/8cad269c90393857e2ebc36223472ec68a5e2401/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/ServiceBrokerAutoConfigurationTest.java#L89
     */
    @Test
    public void loads_yml_env_vars_as_catalog_bean() throws Exception {
        this.contextRunner
                .withUserConfiguration(NoCatalogBeanConfiguration.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(Catalog.class);
                    Catalog catalog = context.getBean(Catalog.class);
                    assertThat(catalog.getServiceDefinitions()).hasSize(1);
                    assertThat(catalog.getServiceDefinitions().get(0).getId()).isEqualTo("ondemand-service");
                    assertThat(catalog.getServiceDefinitions().get(0).getName()).isEqualTo("ondemand");
                    assertThat(catalog.getServiceDefinitions().get(0).getDescription()).isEqualTo("try a simple ondemand service broker implementation");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans()).hasSize(1);
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getId()).isEqualTo("ondemand-plan");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getName()).isEqualTo("default");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getDescription()).isEqualTo("This is a default ondemand plan.  All services are created equally.");
                });
    }
}