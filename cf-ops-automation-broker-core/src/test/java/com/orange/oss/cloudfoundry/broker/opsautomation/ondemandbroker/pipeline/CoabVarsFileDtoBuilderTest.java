package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CoabVarsFileDtoBuilderTest {

	CoabVarsFileDtoBuilder builder = new CoabVarsFileDtoBuilder();

	@Test
	void includesMaintenanceInfoFromCreateRequest() {
		//Given a create request
		CreateServiceInstanceRequest request = OsbBuilderHelper.aCreateServiceInstanceRequest();
		assertThat(request.getMaintenanceInfo()).isNotNull();

		//when
		CoabVarsFileDto coabVarsFileDto = builder.wrapCreateOsbIntoVarsDto(request, "m_1234");

		//then
		assertThat(coabVarsFileDto.maintenanceInfo).isEqualTo(request.getMaintenanceInfo());
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