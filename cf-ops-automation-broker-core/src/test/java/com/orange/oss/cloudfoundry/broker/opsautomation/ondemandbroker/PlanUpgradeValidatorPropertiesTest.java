package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PlanUpgradeValidatorPropertiesTest {

	//See https://docs.spring.io/spring-boot/docs/2.1.12.RELEASE/reference/html/boot-features-developing-auto-configuration.html#boot-features-test-autoconfig
	ConditionEvaluationReportLoggingListener conditionEvaluationReportLoggingListener = new ConditionEvaluationReportLoggingListener(
		LogLevel.INFO);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withInitializer(conditionEvaluationReportLoggingListener)
		.withUserConfiguration(LoadPropertiesConfiguration.class);

	@Configuration
	@EnableConfigurationProperties(PlanUpgradeValidatorProperties.class)
	public static class LoadPropertiesConfiguration {

	}

	@Test
	void reads_rejectedMessageTemplate() {
		this.contextRunner
			.withPropertyValues(
				"plans.upgrade.rejectedMessageTemplate=custom message {0} {1}"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(PlanUpgradeValidatorProperties.class);
				PlanUpgradeValidatorProperties planUpgradeCheckerProperties = context.getBean(
					PlanUpgradeValidatorProperties.class);
				assertThat(planUpgradeCheckerProperties.getRejectedMessageTemplate()).isEqualTo("custom message {0} {1}");
			});
	}

	@Test
	void reads_supportedFromTo() {
		this.contextRunner
			.withPropertyValues(
				"plans.upgrade.supportedFromTo[small][0]=medium",
				"plans.upgrade.supportedFromTo[small][1]=large",
				"plans.upgrade.supportedFromTo[medium][0]=large"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(PlanUpgradeValidatorProperties.class);
				PlanUpgradeValidatorProperties planUpgradeCheckerProperties = context.getBean(
					PlanUpgradeValidatorProperties.class);
				//noinspection ArraysAsListWithZeroOrOneArgument
				assertThat(planUpgradeCheckerProperties.getSupportedFromTo())
					.containsExactly(
						entry("small", asList("medium", "large")),
						entry("medium", asList("large")));
			});
	}

	@Test
	void has_defaults() {
		this.contextRunner
			.withPropertyValues()
			.run((context) -> {
				assertThat(context).hasSingleBean(PlanUpgradeValidatorProperties.class);
				PlanUpgradeValidatorProperties planUpgradeCheckerProperties = context.getBean(
					PlanUpgradeValidatorProperties.class);
				assertThat(planUpgradeCheckerProperties.getSupportedFromTo()).isEmpty();
				assertThat(planUpgradeCheckerProperties.getRejectedMessageTemplate()).isNotBlank();
			});
	}

}