package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentPropertiesTest {

	//See https://docs.spring.io/spring-boot/docs/2.1.12.RELEASE/reference/html/boot-features-developing-auto-configuration.html#boot-features-test-autoconfig
	ConditionEvaluationReportLoggingListener conditionEvaluationReportLoggingListener = new ConditionEvaluationReportLoggingListener(
		LogLevel.INFO);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withInitializer(conditionEvaluationReportLoggingListener)
		.withUserConfiguration(LoadPropertiesConfiguration.class);


	@Configuration
	@EnableConfigurationProperties(DeploymentProperties.class)
	public static class LoadPropertiesConfiguration {

	}

	@Test
	void defaults_readonlymessage_props_when_unspecified() {
		this.contextRunner
			.withPropertyValues(
				"deployment.brokerDisplayName=aDisplayName"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(DeploymentProperties.class);
				DeploymentProperties deploymentProperties = context.getBean(DeploymentProperties.class);
				assertThat(deploymentProperties.getBrokerDisplayName()).isEqualTo("aDisplayName");
				assertThat(deploymentProperties.getServiceInstanceReadOnlyMessage()).isEqualTo(DeploymentProperties.DEFAULT_READ_ONLY_MESSAGE);
				assertThat(deploymentProperties.isServiceInstanceReadOnlyMode()).isEqualTo(false);
			});
	}

}