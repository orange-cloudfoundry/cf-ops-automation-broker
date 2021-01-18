package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class RejectedPlanUpgradeMessageFormatterTest {

	RejectedPlanUpgradeMessageFormatter formatter = new RejectedPlanUpgradeMessageFormatter(
		"Upgrade from {0} to {1} not supported. List of supported plan upgrades from {0} is: {2}");


	@Test
	void rejected_message_when_no_upgrades_supported() {
		//Given
		List<String> emptySupportedUpgrades = new ArrayList<>();
		//when
		String rejectionMessage = formatter.formatRejectionMessage("plan1", "plan2", emptySupportedUpgrades);
		//then
		assertThat(rejectionMessage).isEqualTo("Upgrade from plan1 to plan2 not supported. List of supported plan " +
			"upgrades from plan1 is: none");
	}

	@Test
	void rejected_message_when_upgrades_supported() {
		//Given
		List<String> supportedFromTo = asList("large", "xlarge");
		//when
		String rejectionMessage = formatter.formatRejectionMessage("medium", "small", supportedFromTo);
		//then
		assertThat(rejectionMessage).isEqualTo("Upgrade from medium to small not supported. List of supported plan upgrades from medium is: [large, xlarge]");
	}


}