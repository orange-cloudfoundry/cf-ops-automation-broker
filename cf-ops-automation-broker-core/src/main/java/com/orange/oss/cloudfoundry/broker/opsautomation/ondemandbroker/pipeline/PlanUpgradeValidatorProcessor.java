package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.PlanUpgradeValidatorProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.util.Assert;

public class PlanUpgradeValidatorProcessor extends DefaultBrokerProcessor {

	private static final Logger logger = LoggerFactory.getLogger(PlanUpgradeValidatorProcessor.class.getName());

	private final Map<String, List<String>> supportedFromTo;

	private final RejectedPlanUpgradeMessageFormatter rejectedPlanUpgradeMessageFormatter;

	public PlanUpgradeValidatorProcessor(PlanUpgradeValidatorProperties planUpgradeCheckerProperties) {
		super();
		supportedFromTo = planUpgradeCheckerProperties.getSupportedFromTo();
		String rejectedMessageTemplate = planUpgradeCheckerProperties.getRejectedMessageTemplate();
		Assert.notNull(supportedFromTo, "supportedFromTo is mandatory");
		rejectedPlanUpgradeMessageFormatter = new RejectedPlanUpgradeMessageFormatter(
			rejectedMessageTemplate);
	}

	@Override
	public void preUpdate(Context ctx) {
		UpdateServiceInstanceRequest updateRequest =
			(UpdateServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST);
		Plan toPlan = updateRequest.getPlan();
		UpdateServiceInstanceRequest.PreviousValues previousValues = updateRequest.getPreviousValues();
		if (previousValues == null) {
			//Assume trusted CF CC_NG client which would not request plan upgrade without specifying previous value
			//			throw new ServiceBrokerInvalidParametersException("missing expected 'previous_values' field from CF CC_NG");
			logger.debug("Receiving update request without previous value, assuming params-only update");
			return;
		}
		String previousValuesPlanId = previousValues.getPlanId();
		if (previousValuesPlanId == null) {
			logger.debug("Receiving update request without previous plan_id, assuming params-only update");
			return;
		}
		if (previousValuesPlanId.equals(toPlan.getId())) {
			logger.debug("Receiving update request without plan change, assuming params-only update, or " +
				"maintenance_info upgrade. Accepting the request");
			return;
		}
		ServiceDefinition serviceDefinition = updateRequest.getServiceDefinition();
		Plan fromPlan = serviceDefinition.getPlans().stream()
			.filter((s) -> s.getId().equals(previousValuesPlanId))
			.findAny()
			.orElse(null);
		if (fromPlan == null) {
			logger.warn("Receiving update request without unknown previous plan_id {}. Did catalog changed and " +
				"removed plan ? Catalog is {}", previousValuesPlanId, serviceDefinition);
			return;
		}
		String fromPlanName = fromPlan.getName();
		String toPlanName = toPlan.getName();
		if (supportedFromTo.isEmpty()) {
			logger.debug("No plan upgrades supported at all");
			String formattedMessage= rejectedPlanUpgradeMessageFormatter
				.formatRejectionMessage(fromPlanName, toPlanName, Collections.emptyList());
			throw new ServiceInstanceUpdateNotSupportedException(formattedMessage);
		}
		List<String> supportedTargetPlansFromPrevious = supportedFromTo.get(fromPlanName);
		if (supportedTargetPlansFromPrevious == null) {
			logger.debug("No support plan upgrade from {} to {}", fromPlanName, toPlanName);
			String formattedMessage= rejectedPlanUpgradeMessageFormatter
				.formatRejectionMessage(fromPlanName, toPlanName, Collections.emptyList());
			throw new ServiceInstanceUpdateNotSupportedException(formattedMessage);
		}
		logger.debug("validating whether plan upgrade from {} to {} is supported within {}",
			fromPlanName, toPlanName, supportedTargetPlansFromPrevious);
		if (! supportedTargetPlansFromPrevious.contains(toPlanName)) {
			String formattedMessage= rejectedPlanUpgradeMessageFormatter
				.formatRejectionMessage(fromPlanName, toPlanName, supportedTargetPlansFromPrevious);
			throw new ServiceInstanceUpdateNotSupportedException(formattedMessage);
		}
		logger.debug("plan upgrade is validated");
	}

}
