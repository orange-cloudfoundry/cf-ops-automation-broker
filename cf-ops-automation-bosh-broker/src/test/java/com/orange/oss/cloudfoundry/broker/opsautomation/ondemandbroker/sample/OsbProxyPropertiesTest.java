package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OsbProxyPropertiesTest {

	//See https://docs.spring.io/spring-boot/docs/2.1.12.RELEASE/reference/html/boot-features-developing-auto-configuration.html#boot-features-test-autoconfig
	ConditionEvaluationReportLoggingListener conditionEvaluationReportLoggingListener = new ConditionEvaluationReportLoggingListener(
		LogLevel.INFO);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withInitializer(conditionEvaluationReportLoggingListener)
		.withUserConfiguration(LoadPropertiesConfiguration.class);

	@Configuration
	@EnableConfigurationProperties(OsbProxyProperties.class)
	public static class LoadPropertiesConfiguration {

	}

	@Test
	void reads_singular_fields() {
		this.contextRunner
			.withPropertyValues(
				"pipeline.osbDelegateUser=user",
				"pipeline.osbDelegatePassword=password",
				"pipeline.maxExecutionDurationSeconds=1000",
				"#{0} resolves to the cf service instance ID",
				"#pipeline.brokerUrlPattern=https://cassandra-broker_{0}.mydomain/com",
				"#8089 is wiremock server",
				"pipeline.brokerUrlPattern=https://localhost:8089/"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(OsbProxyProperties.class);
				OsbProxyProperties osbProxyProperties = context.getBean(OsbProxyProperties.class);
				assertThat(osbProxyProperties.getOsbDelegateUser()).isEqualTo("user");
				assertThat(osbProxyProperties.getOsbDelegatePassword()).isEqualTo("password");
				assertThat(osbProxyProperties.getBrokerUrlPattern()).isEqualTo("https://localhost:8089/");
			});
	}

}