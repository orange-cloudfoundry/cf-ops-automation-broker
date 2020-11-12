package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class CoabVarsFileDtoSampleHelper {

	protected static CoabVarsFileDto aTypicalUserProvisionningRequest() {
		CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
		coabVarsFileDto.deployment_name = "cassandravarsops_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
		coabVarsFileDto.instance_id = "service_instance_id";
		coabVarsFileDto.service_id = "service_definition_id";
		coabVarsFileDto.plan_id = "plan_guid";

		coabVarsFileDto.context.user_guid = "user_guid1";
		coabVarsFileDto.context.space_guid = "space_guid1";
		coabVarsFileDto.context.organization_guid = "org_guid1";
		return coabVarsFileDto;
	}

}
