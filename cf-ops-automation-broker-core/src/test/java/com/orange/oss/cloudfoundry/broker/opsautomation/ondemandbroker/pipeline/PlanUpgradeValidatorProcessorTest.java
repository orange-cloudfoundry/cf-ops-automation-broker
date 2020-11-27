package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.PlanUpgradeCheckerProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.LARGE_PLAN_NAME;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.MEDIUM_PLAN_NAME;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.MEDIUM_SERVICE_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.SMALL_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.SMALL_PLAN_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("LambdaCanBeReplacedWithAnonymous")
class PlanUpgradeValidatorProcessorTest {



	@Test
	void accepts_supported_plan_upgrade() {
		//Given a validator
		PlanUpgradeCheckerProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given an update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.aPlanUpdateServiceInstanceRequest();
		assertThat(request.getPlan().getName()).isEqualTo(MEDIUM_PLAN_NAME);
		assertThat(request.getPreviousValues().getPlanId()).isEqualTo(SMALL_PLAN_ID);

		//Given a populated context
		Context context = new Context();
		context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);

		//When
		planUpgradeValidatorProcessor.preUpdate(context);

		//Then no exception is thrown
	}

	@Test
	void rejects_unsupported_plan_upgrades() {
		//Given a validator
		PlanUpgradeCheckerProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given an update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.aPlanDowngradeServiceInstanceRequest();
		assertThat(request.getPlan().getName()).isEqualTo(SMALL_PLAN_NAME);
		assertThat(request.getPreviousValues().getPlanId()).isEqualTo(MEDIUM_SERVICE_PLAN_ID);

		//Given a populated context
		Context context = new Context();
		context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);

		//When
		assertThatThrownBy(() -> {planUpgradeValidatorProcessor.preUpdate(context);})
			.isInstanceOf(ServiceInstanceUpdateNotSupportedException.class)
			.hasMessageContaining("upgrade from ...");

	}

	@Test
	void rejects_any_plan_upgrades_by_default() {
		//Given a validator
		PlanUpgradeCheckerProperties planUpgradeCheckerProperties = new PlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given an update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.aPlanDowngradeServiceInstanceRequest();
		assertThat(request.getPlan().getName()).isEqualTo(SMALL_PLAN_NAME);
		assertThat(request.getPreviousValues().getPlanId()).isEqualTo(MEDIUM_SERVICE_PLAN_ID);

		//Given a populated context
		Context context = new Context();
		context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);

		//When
		assertThatThrownBy(() -> {planUpgradeValidatorProcessor.preUpdate(context);})
			.isInstanceOf(ServiceInstanceUpdateNotSupportedException.class)
			.hasMessageContaining("Service instance update not supported");
	}


	@Nonnull
	private PlanUpgradeCheckerProperties aPlanUpgradeCheckerProperties() {
		PlanUpgradeCheckerProperties planUpgradeCheckerProperties = new PlanUpgradeCheckerProperties();
		HashMap<String, List<String>> supportedFromTo = aSupportedMatrix();
		planUpgradeCheckerProperties.setSupportedFromTo(supportedFromTo);
		planUpgradeCheckerProperties.setRejectedMessageTemplate("upgrade from ...");
		return planUpgradeCheckerProperties;
	}

	@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
	@Nonnull
	private HashMap<String, List<String>> aSupportedMatrix() {
		HashMap<String, List<String>> supportedFromTo = new HashMap<>();
		supportedFromTo.put(SMALL_PLAN_NAME, asList(MEDIUM_PLAN_NAME, LARGE_PLAN_NAME));
		supportedFromTo.put(MEDIUM_PLAN_NAME, asList(LARGE_PLAN_NAME));
		return supportedFromTo;
	}



}