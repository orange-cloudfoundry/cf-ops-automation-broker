package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

@SuppressWarnings("ConstantConditions")
public class YamlCataloglAsEnvironmentVarApplicationContextInitializerTest {

    @BeforeEach
    public void init() {
        System.setProperty("CATALOG_YML", CATALOG_YML);
        assertThat(System.getProperty("CATALOG_YML")).isNotEmpty();
    }

    @AfterEach
    public void after() {
        System.clearProperty("CATALOG_YML");
        assertThat(System.getProperty("CATALOG_YML")).isNull();
    }

    private final YamlCataloglAsEnvironmentVarApplicationContextInitializer contextInitializer = new YamlCataloglAsEnvironmentVarApplicationContextInitializer();

    public static final String CATALOG_YML = "servicebroker:\n" +
            "   catalog:\n" +
            "     services:\n" +
            "       - id: ondemand-service\n" +
            "         name: ondemand\n" +
            "         description: try a simple ondemand service broker implementation\n" +
            "         bindable: true\n" +
            "         plans:\n" +
            "           - id: ondemand-plan\n" +
            "             name: default\n" +
            "             description: This is a default ondemand plan.  All services are created equally.\n" +
            "         tags:\n" +
            "           -ondemand\n" +
            "           -document\n" +
            "         metadata:\n" +
            "           displayName: ondemand\n" +
            "           imageUrl: https://orange.com/image.png\n" +
            "           longDescription: ondemand Service\n" +
            "           providerDisplayName: Orange\n" +
            "           documentationUrl: https://orange.com/doc\n" +
            "           supportUrl: https://orange.com/support\n";

    @Test
    public void loads_yml_env_var_as_property_source_into_spring_context_and_convert_to_scosb_format() {
        //given a CATALOG_YML env var is defined
        StaticApplicationContext context = new StaticApplicationContext();

        //when
        contextInitializer.initialize(context);

        //then a property source is indeed added
        assertThat(context.getEnvironment().getPropertySources().contains("catalog_from_env_var")).isTrue();

        //and properties within this entries have been converted
        PropertySource<?> catalogFromEnvVar = context.getEnvironment().getPropertySources().get("catalog_from_env_var");
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].id")).isEqualTo("ondemand-service");
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].name")).isEqualTo("ondemand");
        // same with nested properties
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].metadata.imageUrl")).isEqualTo("https://orange.com/image.png");
    }

    @Test
    public void converts_legacy_to_scosb_keys() {
        //given
        Map<String, String> source = new HashMap<>();
        source.put("servicebroker.catalog.services[0].id", "ondemand-service");
        source.put("servicebroker.catalog.services[0].name", "ondemand");
        //when
        OriginTrackedMapPropertySource convertedSource = contextInitializer
            .convertPropertySourceToScOsbKeyPrefix(source);
        //then
        assertThat(convertedSource.getSource()).containsOnly(
                entry("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service"),
                entry("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand"));
    }

    @Test
    public void does_not_convert_scosb_keys() {
        //given
        Map<String, String> source = new HashMap<>();
        source.put("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service");
        source.put("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand");
        //when
        contextInitializer.convertPropertySourceToScOsbKeyPrefix(source);
        //then
        assertThat(source).containsOnly(
                entry("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service"),
                entry("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand"));
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
            public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
                return Mono.empty();
            }

            @Override
            public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
                return Mono.empty();
            }

            @Override
            public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
                return Mono.empty();
            }

            @Override
            public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
                return Mono.empty();
            }
        }
    }


    /**
     * Inspired from org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfigurationTest#servicesAreCreatedFromCatalogProperties
     * See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/8cad269c90393857e2ebc36223472ec68a5e2401/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/ServiceBrokerAutoConfigurationTest.java#L89
     */
    @Test
    public void loads_yml_env_vars_as_catalog_bean() {
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

