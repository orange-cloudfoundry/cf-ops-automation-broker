package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.text.MessageFormat;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

public class RejectedPlanUpgradeMessageFormatter {

	private MessageFormat rejectedMessageTemplate;

	public RejectedPlanUpgradeMessageFormatter(@NotNull String rejectedMessageTemplate) {
		Assert.notNull(rejectedMessageTemplate, "rejectedMessageTemplate is mandatory");
		this.rejectedMessageTemplate = new MessageFormat(rejectedMessageTemplate);
	}

	String formatRejectionMessage(String fromPlanName, String toPlanName, List<String> supportedTargetPlans) {
		String supportedPlanUpgrades;
		if (supportedTargetPlans.isEmpty()) {
			supportedPlanUpgrades = "none";
		} else {
			supportedPlanUpgrades = supportedTargetPlans.toString();
		}
		return rejectedMessageTemplate.format(new String[] {fromPlanName, toPlanName, supportedPlanUpgrades});
	}

}
