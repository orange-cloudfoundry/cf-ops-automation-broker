package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.PlanUpgradeValidatorProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.LARGE_PLAN_NAME;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.MEDIUM_PLAN_NAME;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.MEDIUM_SERVICE_PLAN_ID;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper.SMALL_PLAN_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("LambdaCanBeReplacedWithAnonymous")
class PlanUpgradeValidatorProcessorTest {


	/**
	 * a `cf update-service --upgrade` is accepted
	 */
	@Test
	void accepts_any_maintenance_info_upgrade() {
		//Given a validator
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given a noop upgrade update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.anUpgradeServiceInstanceRequest();
		assertThat(request.getPreviousValues().getPlanId()).isEqualTo(request.getPlan().getId());

		//Given a populated context
		Context context = new Context();
		context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);

		//When
		planUpgradeValidatorProcessor.preUpdate(context);

		//Then no exception is thrown
	}

	/**
	 * e.g. a non compliant OSB client which does not provide the previous_value field
	 */
	@Test
	void accepts_any_update_without_previous_value() {
		//Given a validator
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given a noop upgrade update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.anUpdateServiceInstanceRequestWithoutPreviousValue();
		assertThat(request.getPreviousValues()).isNull();

		//Given a populated context
		Context context = new Context();
		context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);

		//When
		planUpgradeValidatorProcessor.preUpdate(context);

		//Then no exception is thrown
	}
	@Test
	void accepts_supported_plan_upgrade() {
		//Given a validator
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
		PlanUpgradeValidatorProcessor planUpgradeValidatorProcessor = new PlanUpgradeValidatorProcessor(planUpgradeCheckerProperties);

		//Given an update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.aPlanUpdateServiceInstanceRequest();
		assertThat(request.getPlan().getName()).isEqualTo(LARGE_PLAN_NAME);
		assertThat(request.getPreviousValues().getPlanId()).isEqualTo(MEDIUM_SERVICE_PLAN_ID);

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
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = aPlanUpgradeCheckerProperties();
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
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = new PlanUpgradeValidatorProperties();
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


	@NotNull
	private PlanUpgradeValidatorProperties aPlanUpgradeCheckerProperties() {
		PlanUpgradeValidatorProperties planUpgradeCheckerProperties = new PlanUpgradeValidatorProperties();
		HashMap<String, List<String>> supportedFromTo = aSupportedMatrix();
		planUpgradeCheckerProperties.setSupportedFromTo(supportedFromTo);
		planUpgradeCheckerProperties.setRejectedMessageTemplate("upgrade from ...");
		return planUpgradeCheckerProperties;
	}

	@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
	@NotNull
	private HashMap<String, List<String>> aSupportedMatrix() {
		HashMap<String, List<String>> supportedFromTo = new HashMap<>();
		supportedFromTo.put(SMALL_PLAN_NAME, asList(MEDIUM_PLAN_NAME, LARGE_PLAN_NAME));
		supportedFromTo.put(MEDIUM_PLAN_NAME, asList(LARGE_PLAN_NAME));
		return supportedFromTo;
	}



}