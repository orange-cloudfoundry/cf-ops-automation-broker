package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.instance.AsyncParameterizedServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

public class CoabVarsFileDtoBuilder {

	public String extractUserKeyFromOsbContext(org.springframework.cloud.servicebroker.model.Context context) {
		if (context instanceof CloudFoundryContext) {
			CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
			return (String) cloudFoundryContext.getProperty(OsbConstants.ORIGINATING_USER_KEY);
		}
		return null;
	}

	public String extractCfSpaceGuid(AsyncParameterizedServiceInstanceRequest request) {
		org.springframework.cloud.servicebroker.model.Context context = request.getContext();
		if (context instanceof CloudFoundryContext) {
			CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
			return cloudFoundryContext.getSpaceGuid();
		}
		return null;
	}

	public String extractCfOrgGuid(AsyncParameterizedServiceInstanceRequest request) {
		org.springframework.cloud.servicebroker.model.Context context = request.getContext();
		if (context instanceof CloudFoundryContext) {
			CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
			return cloudFoundryContext.getOrganizationGuid();
		}
		return null;
	}

	public CoabVarsFileDto wrapGenericOsbIntoVarsDto(AsyncParameterizedServiceInstanceRequest request,
		String serviceInstanceId,
		String serviceDefinitionId, String planId, String deploymentName) {

		String userKey = extractUserKeyFromOsbContext(request.getOriginatingIdentity());
		String organizationGuid = extractCfOrgGuid(request);
		String spaceGuid = extractCfSpaceGuid(request);

		CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
		coabVarsFileDto.deployment_name = deploymentName;
		coabVarsFileDto.instance_id = serviceInstanceId;
		coabVarsFileDto.service_id = serviceDefinitionId;
		coabVarsFileDto.plan_id = planId;

		coabVarsFileDto.context.user_guid = userKey;
		coabVarsFileDto.context.space_guid = spaceGuid;
		coabVarsFileDto.context.organization_guid = organizationGuid;
		if (request.getParameters() != null) {
			coabVarsFileDto.parameters.putAll(request.getParameters());
		}
		return coabVarsFileDto;
	}

	public CoabVarsFileDto wrapCreateOsbIntoVarsDto(CreateServiceInstanceRequest request, String deploymentName) {
		String serviceInstanceId = request.getServiceInstanceId();
		String serviceDefinitionId = request.getServiceDefinitionId();
		String planId = request.getPlanId();

		return wrapGenericOsbIntoVarsDto(request, serviceInstanceId, serviceDefinitionId, planId,
			deploymentName);
	}

	public CoabVarsFileDto wrapUpdateOsbIntoVarsDto(UpdateServiceInstanceRequest request, String deploymentName) {
		String serviceInstanceId = request.getServiceInstanceId();
		String serviceDefinitionId = request.getServiceDefinitionId();
		String planId = request.getPlanId();

		return wrapGenericOsbIntoVarsDto(request, serviceInstanceId, serviceDefinitionId, planId,
			deploymentName);
	}

}
