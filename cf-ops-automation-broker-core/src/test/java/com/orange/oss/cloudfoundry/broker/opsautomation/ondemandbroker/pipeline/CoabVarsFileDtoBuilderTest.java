package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CoabVarsFileDtoBuilderTest {

	CoabVarsFileDtoBuilder builder = new CoabVarsFileDtoBuilder();

	@Test
	void includesMaintenanceInfoFromCreateRequest() {

	}

	@Test
	void includesMaintenanceInfoFromUpdateRequest() {
		//Given an update request
		UpdateServiceInstanceRequest request = OsbBuilderHelper.anUpdateServiceInstanceRequest();
		assertThat(request.getMaintenanceInfo()).isNotNull();

		//when
		CoabVarsFileDto coabVarsFileDto = builder.wrapUpdateOsbIntoVarsDto(request, "m_1234");

		//then
		assertThat(coabVarsFileDto.maintenanceInfo).isEqualTo(request.getMaintenanceInfo());
	}

}