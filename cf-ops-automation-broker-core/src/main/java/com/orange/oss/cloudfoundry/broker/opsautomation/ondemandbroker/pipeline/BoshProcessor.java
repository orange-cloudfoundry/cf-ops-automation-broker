package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceBindingService;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.AsyncParameterizedServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;

public class BoshProcessor extends DefaultBrokerProcessor {

    public static final String X_OSB_CMDB_CUSTOM_KEY_NAME = "x-osb-cmdb";

    public static final String CMDB_LABELS_KEY = "labels";

    public static final String CMDB_BROKERED_SERVICE_INSTANCE_GUID_KEY = "brokered_service_instance_guid";

    private static Logger logger = LoggerFactory.getLogger(BoshProcessor.class.getName());
    private final String brokerDisplayName;
    private String modelDeploymentShortAlias;
    private String modelDeploymentSeparator;
    private String dashboardUrlTemplate;

    private String templatesRepositoryAliasName;
    private String secretsRepositoryAliasName;
    private PipelineCompletionTracker tracker;
    private TemplatesGenerator templatesGenerator;
    private SecretsGenerator secretsGenerator;

    public BoshProcessor(String templatesRepositoryAliasName, String secretsRepositoryAliasName, TemplatesGenerator templatesGenerator, SecretsGenerator secretsGenerator, PipelineCompletionTracker tracker, String brokerDisplayName, String modelDeploymentShortAlias, String modelDeploymentSeparator, String dashboardUrlTemplate) {
        this.templatesRepositoryAliasName = templatesRepositoryAliasName;
        this.secretsRepositoryAliasName = secretsRepositoryAliasName;
        this.templatesGenerator = templatesGenerator;
        this.secretsGenerator = secretsGenerator;
        this.tracker = tracker;
        this.brokerDisplayName = brokerDisplayName;
        this.modelDeploymentShortAlias = modelDeploymentShortAlias;
        this.modelDeploymentSeparator = modelDeploymentSeparator;
        this.dashboardUrlTemplate = dashboardUrlTemplate;
    }

    @Override
    public void preCreate(Context ctx) {
        //Need to retrieve workdirs from context (one for secrets and one for template)
        Path templatesWorkDir = getPaasTemplate(ctx);
        Path secretsWorkDir = getPaasSecret(ctx);

        //Need to retrieve service instance id from  context
        CreateServiceInstanceRequest creationRequest = (CreateServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
        String serviceInstanceId = creationRequest.getServiceInstanceId();
        logger.debug("service instance id is " + serviceInstanceId);

        CoabVarsFileDto coabVarsFileDto = wrapCreateOsbIntoVarsDto(creationRequest);

        //Check pre-requisites and generate paas-template structure
        this.templatesGenerator.checkPrerequisites(templatesWorkDir);
        this.templatesGenerator.generate(templatesWorkDir, serviceInstanceId, coabVarsFileDto);

        //Check pre-requisites and generate paas-secrets structure
        this.secretsGenerator.checkPrerequisites(secretsWorkDir);
        this.secretsGenerator.generate(secretsWorkDir, serviceInstanceId, null);

        //Create response and put it into context
        CreateServiceInstanceResponse creationResponse = CreateServiceInstanceResponse.builder().
                async(true).
                dashboardUrl(formatDashboard(dashboardUrlTemplate, creationRequest)).
                operation(this.tracker.getPipelineOperationStateAsJson(creationRequest)).build();
        ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, creationResponse);

        //Generate commit message and put it into context
        setCommitMsg(ctx, formatProvisionCommitMsg(creationRequest));
    }

    @Override
    public void preGetLastOperation(Context ctx) {

        //Need to retrieve workdir from context
        Path secretsWorkDir = getPaasSecret(ctx);

        //Need to retrieve operation request from context
        GetLastServiceOperationRequest operationRequest = (GetLastServiceOperationRequest)
                ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST);

        //Get deployment execution status through response
        GetLastServiceOperationResponse operationResponse = this.tracker.getDeploymentExecStatus(secretsWorkDir, operationRequest.getServiceInstanceId(), operationRequest.getOperation(), operationRequest);

        //Put the response into context
        ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, operationResponse);
    }

    @Override
    public void preBind(Context ctx) {
        //retrieve workdir from context
        Path secretsWorkDir = getPaasSecret(ctx);

        //retrieve request from context
        CreateServiceInstanceBindingRequest request = (CreateServiceInstanceBindingRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_REQUEST);

        CreateServiceInstanceBindingResponse response = this.tracker.delegateBindRequest(secretsWorkDir, request);

        //Put the response into context
        ctx.contextKeys.put(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_RESPONSE, response);
    }

    @Override
    public void preUnBind(Context ctx) {
        //retrieve workdir from context
        Path secretsWorkDir = getPaasSecret(ctx);

        //retrieve request from context
        DeleteServiceInstanceBindingRequest request = (DeleteServiceInstanceBindingRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceBindingService.DELETE_SERVICE_INSTANCE_BINDING_REQUEST);

        this.tracker.delegateUnbindRequest(secretsWorkDir, request);
    }

    @Override
    public void preDelete(Context ctx) {

        //Need to retrieve workdir from context
        Path secretsWorkDir = getPaasSecret(ctx);
        Path templatesWorkDir = getPaasTemplate(ctx);

        //Need to retrieve delete request from context
        DeleteServiceInstanceRequest request = (DeleteServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST);
        String serviceInstanceId = request.getServiceInstanceId();

        //Check pre-requisites and remove paas-secrets structure
        this.secretsGenerator.checkPrerequisites(secretsWorkDir);
        this.secretsGenerator.remove(secretsWorkDir, serviceInstanceId);

        //Check pre-requisites and remove paas-templates structure
        this.templatesGenerator.checkPrerequisites(templatesWorkDir);
        this.templatesGenerator.remove(templatesWorkDir, serviceInstanceId);

        //Create response and put it into context
        DeleteServiceInstanceResponse deletionResponse = DeleteServiceInstanceResponse.builder()
                .async(true)
                .operation(this.tracker.getPipelineOperationStateAsJson(request)).build();

        ctx.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE, deletionResponse);

        //Generate commit message and put it into context
        setCommitMsg(ctx, formatUnprovisionCommitMsg(request));
    }

    protected String formatProvisionCommitMsg(CreateServiceInstanceRequest request) {
        String originDetails;

        Object userKey = extractUserKeyFromOsbContext(request.getOriginatingIdentity());

        String organizationGuid = extractCfOrgGuid(request);
        String spaceGuid = extractCfSpaceGuid(request);

        originDetails = "Requested from space_guid=" + spaceGuid + " org_guid=" + organizationGuid + " by user_guid=" + userKey;
        return brokerDisplayName + " broker: create instance id=" + request.getServiceInstanceId()
                + "\n\n" + originDetails;
    }

    private String extractCfSpaceGuid(AsyncParameterizedServiceInstanceRequest request) {
        org.springframework.cloud.servicebroker.model.Context context = request.getContext();
        if (context instanceof CloudFoundryContext) {
            CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
            return cloudFoundryContext.getSpaceGuid();
        }
        return null;
    }

    private String extractCfOrgGuid(AsyncParameterizedServiceInstanceRequest request) {
        org.springframework.cloud.servicebroker.model.Context context = request.getContext();
        if (context instanceof CloudFoundryContext) {
            CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
            return cloudFoundryContext.getOrganizationGuid();
        }
        return null;
    }

    protected String formatUnprovisionCommitMsg(DeleteServiceInstanceRequest request) {
        Object userKey = extractUserKeyFromOsbContext(request.getOriginatingIdentity());

        return brokerDisplayName + " broker: delete instance id=" + request.getServiceInstanceId()
                + "\n\nRequested by user_guid=" + userKey;
    }

    private String extractUserKeyFromOsbContext(org.springframework.cloud.servicebroker.model.Context context) {
        if (context instanceof CloudFoundryContext) {
            CloudFoundryContext cloudFoundryContext = (CloudFoundryContext) context;
            return (String) cloudFoundryContext.getProperty(OsbConstants.ORIGINATING_USER_KEY);
        }
        return null;
    }

    private Path getPaasSecret(Context ctx) {
        Path secretsWorkDir = (Path) ctx.contextKeys.get(secretsRepositoryAliasName + GitProcessorContext.workDir.toString());
        logger.debug("secrets workDir is " + secretsWorkDir.toString());
        return secretsWorkDir;
    }

    private Path getPaasTemplate(Context ctx) {
        Path templatesWorkDir = (Path) ctx.contextKeys.get(templatesRepositoryAliasName + GitProcessorContext.workDir.toString());
        logger.debug("templates workDir is " + templatesWorkDir.toString());
        return templatesWorkDir;
    }

    /**
     * Set commmit msg for both secrets and template repos. Template repos commit msg will be ignored if not changes
     */
    private void setCommitMsg(Context ctx, String msg) {
        ctx.contextKeys.put(templatesRepositoryAliasName + GitProcessorContext.commitMessage.toString(), msg);
        ctx.contextKeys.put(secretsRepositoryAliasName + GitProcessorContext.commitMessage.toString(), msg);
    }

    protected CoabVarsFileDto wrapCreateOsbIntoVarsDto(CreateServiceInstanceRequest request) {
        String serviceInstanceId = request.getServiceInstanceId();
        String serviceDefinitionId = request.getServiceDefinitionId();
        String planId = request.getPlanId();

        return wrapGenericOsbIntoVarsDto(request, serviceInstanceId, serviceDefinitionId, planId);
    }

    private CoabVarsFileDto wrapGenericOsbIntoVarsDto(AsyncParameterizedServiceInstanceRequest request, String serviceInstanceId,
        String serviceDefinitionId, String planId) {

        String userKey = extractUserKeyFromOsbContext(request.getOriginatingIdentity());
        String organizationGuid = extractCfOrgGuid(request);
        String spaceGuid = extractCfSpaceGuid(request);

        CoabVarsFileDto coabVarsFileDto = new CoabVarsFileDto();
        coabVarsFileDto.deployment_name = modelDeploymentShortAlias + modelDeploymentSeparator + serviceInstanceId;
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

    public String formatDashboard(String dashboardUrlTemplate, CreateServiceInstanceRequest request) {
        if (dashboardUrlTemplate == null) {
            logger.debug("No dashboard template url configured, returning null dashboardUrl");
            return null;
        }
        String serviceInstanceId = request.getServiceInstanceId();
        String brokered_service_instance_guid = null;
        Map<String, Object> parameters = request.getParameters();
        if (parameters != null) {
            //noinspection unchecked
            Map<String,Map<String,String>> osbCmdbCustomParamValue = (Map<String, Map<String, String>>) parameters.get(X_OSB_CMDB_CUSTOM_KEY_NAME);
            if (osbCmdbCustomParamValue != null) {
                Map<String, String> labels = osbCmdbCustomParamValue.get(CMDB_LABELS_KEY);
                if (labels != null) {
                    brokered_service_instance_guid = labels.get(CMDB_BROKERED_SERVICE_INSTANCE_GUID_KEY);
                }
            }
        }

        if (brokered_service_instance_guid == null) {
            logger.info("Did not find Osb-cmdb custom brokered_service_instance_guid into CSIR {}", request);
        }

        String formattedDashboardUrl = new MessageFormat(dashboardUrlTemplate).format(new String[] {serviceInstanceId, brokered_service_instance_guid});
        logger.debug("Formatted dashboard url into {} from template {} req guid {} and brokered_service_instance_guid {}",
            formattedDashboardUrl, dashboardUrlTemplate, serviceInstanceId, brokered_service_instance_guid);
        return formattedDashboardUrl;
    }
}

