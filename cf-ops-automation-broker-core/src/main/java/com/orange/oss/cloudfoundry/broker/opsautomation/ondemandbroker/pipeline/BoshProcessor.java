package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceBindingService;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.nio.file.Path;

public class BoshProcessor extends DefaultBrokerProcessor {

	private static Logger logger = LoggerFactory.getLogger(BoshProcessor.class.getName());
	private final String brokerDisplayName;

	private String templatesRepositoryAliasName;
	private String secretsRepositoryAliasName;
	private PipelineCompletionTracker tracker;
	private TemplatesGenerator templatesGenerator;
	private SecretsGenerator secretsGenerator;

	public BoshProcessor(String templatesRepositoryAliasName, String secretsRepositoryAliasName, TemplatesGenerator templatesGenerator, SecretsGenerator secretsGenerator, PipelineCompletionTracker tracker, String brokerDisplayName) {
        this.templatesRepositoryAliasName = templatesRepositoryAliasName;
        this.secretsRepositoryAliasName = secretsRepositoryAliasName;
        this.templatesGenerator = templatesGenerator;
		this.secretsGenerator = secretsGenerator;
		this.tracker = tracker;
		this.brokerDisplayName = brokerDisplayName;
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

		//Check pre-requisites and generate paas-template structure
		this.templatesGenerator.checkPrerequisites(templatesWorkDir);
		this.templatesGenerator.generate(templatesWorkDir, serviceInstanceId);

		//Check pre-requisites and generate paas-secrets structure
		this.secretsGenerator.checkPrerequisites(secretsWorkDir);
		this.secretsGenerator.generate(secretsWorkDir, serviceInstanceId);

		//Create response and put it into context
		CreateServiceInstanceResponse creationResponse = new CreateServiceInstanceResponse();
		creationResponse.withAsync(true);
		creationResponse.withOperation(this.tracker.getPipelineOperationStateAsJson(creationRequest));
		ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, creationResponse);

		//Generate commit message and put it into context
        setCommitMsg(ctx, brokerDisplayName + " broker: create instance id=" + serviceInstanceId);
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

		//Need to retrieve delete request from context
		DeleteServiceInstanceRequest request = (DeleteServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST);
		String serviceInstanceId = request.getServiceInstanceId();

		//Check pre-requisites and generate paas-secrets structure
		this.secretsGenerator.checkPrerequisites(secretsWorkDir);
		this.secretsGenerator.remove(secretsWorkDir, serviceInstanceId);

		//Create response and put it into context
		DeleteServiceInstanceResponse deletionResponse = new DeleteServiceInstanceResponse();
		deletionResponse.withAsync(true);
		deletionResponse.withOperation(this.tracker.getPipelineOperationStateAsJson(request));

		ctx.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE, deletionResponse);

		//Generate commit message and put it into context
        setCommitMsg(ctx,brokerDisplayName+" broker: delete instance id=" + serviceInstanceId);
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

}
