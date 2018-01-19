package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.nio.file.Path;
import java.time.Clock;

public class CassandraProcessor extends DefaultBrokerProcessor {

	private static Logger logger = LoggerFactory.getLogger(CassandraProcessor.class.getName());

	private String templatesRepositoryAliasName;
	private String secretsRepositoryAliasName;
	private PipelineCompletionTracker tracker;
	private TemplatesGenerator templatesGenerator;
	private SecretsGenerator secretsGenerator;

	public CassandraProcessor(String templatesRepositoryAliasName, String secretsRepositoryAliasName, Clock clock) {
		this.templatesRepositoryAliasName = templatesRepositoryAliasName;
		this.secretsRepositoryAliasName = secretsRepositoryAliasName;
		this.tracker = new PipelineCompletionTracker(clock); //TODO : Remove
	}

	public CassandraProcessor(String templatesRepositoryAliasName, String secretsRepositoryAliasName, Clock clock, TemplatesGenerator templatesGenerator, SecretsGenerator secretsGenerator, PipelineCompletionTracker tracker) {
		this(templatesRepositoryAliasName, secretsRepositoryAliasName, clock);
		this.templatesGenerator = templatesGenerator;
		this.secretsGenerator = secretsGenerator;
		this.tracker = tracker;
	}

	@Override
	public void preCreate(Context ctx) {
		//Need to retrieve workdirs from context (one for secrets and one for template)
		Path templatesWorkDir = (Path) ctx.contextKeys.get(templatesRepositoryAliasName + GitProcessorContext.workDir.toString());
		logger.debug("templates workDir is " + templatesWorkDir.toString());

		Path secretsWorkDir = (Path) ctx.contextKeys.get(secretsRepositoryAliasName + GitProcessorContext.workDir.toString());
		logger.debug("secrets workDir is " + secretsWorkDir.toString());

		//Need to retrieve service instance id from  context
		CreateServiceInstanceRequest creationRequest = (CreateServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
		String serviceInstanceId = creationRequest.getServiceInstanceId();
		logger.debug("service instance id is " + serviceInstanceId);

		//Check pre-requisites and generate paas-template structure
		this.templatesGenerator = new TemplatesGenerator(templatesWorkDir, serviceInstanceId);
		//this.templatesGenerator.setWorkDir(templatesWorkDir);
		//this.templatesGenerator.setServiceInstanceId(serviceInstanceId);
		this.templatesGenerator.checkPrerequisites();
		this.templatesGenerator.generate();

		//Check pre-requisites and generate paas-secrets structure
		this.secretsGenerator = new SecretsGenerator(secretsWorkDir, serviceInstanceId);
		//this.secretsGenerator.setWorkDir(secretsWorkDir);
		//this.secretsGenerator.setServiceInstanceId(serviceInstanceId);
		this.secretsGenerator.checkPrerequisites();
		this.secretsGenerator.generate();

		//Create response and put it into context
		CreateServiceInstanceResponse creationResponse = new CreateServiceInstanceResponse();
		creationResponse.withAsync(true);
		//TODO : Must pass the serialized pipeline operation state
		creationResponse.withOperation(CassandraProcessorConstants.OSB_OPERATION_CREATE);
		ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, creationResponse);

		//Generate commit message and put it into context
		String msg = "Cassandra broker" + ": "+ CassandraProcessorConstants.OSB_OPERATION_CREATE + " instance id=" + serviceInstanceId;
		ctx.contextKeys.put(GitProcessorContext.commitMessage.toString(), msg);
	}

	@Override
	public void preGetLastOperation(Context ctx) {

		//Need to retrieve workdir from context
		Path secretsWorkDir = (Path) ctx.contextKeys.get(secretsRepositoryAliasName + GitProcessorContext.workDir.toString());
		logger.debug("secrets workDir is " + secretsWorkDir.toString());

		//Need to retrieve operation request from context
		GetLastServiceOperationRequest operationRequest = (GetLastServiceOperationRequest)
				ctx.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST);

		//Get deployment execution status through response
		GetLastServiceOperationResponse operationResponse = this.tracker.getDeploymentExecStatus(secretsWorkDir, operationRequest.getServiceInstanceId(), operationRequest.getOperation());

		//Put the response into context
		ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, operationResponse);
	}

	@Override
	public void preDelete(Context ctx) {

		//Need to retrieve workdir from context
		Path secretsWorkDir = (Path) ctx.contextKeys.get(secretsRepositoryAliasName + GitProcessorContext.workDir.toString());
		logger.debug("secrets workDir is " + secretsWorkDir.toString());

		//Need to retrieve delete request from context
		DeleteServiceInstanceRequest request = (DeleteServiceInstanceRequest) ctx.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST);
		String serviceInstanceId = request.getServiceInstanceId();

		//Check pre-requisites and generate paas-secrets structure
		this.secretsGenerator = new SecretsGenerator(secretsWorkDir, serviceInstanceId);
		//this.secretsGenerator.setWorkDir(secretsWorkDir);
		//this.secretsGenerator.setServiceInstanceId(serviceInstanceId);
		this.secretsGenerator.checkPrerequisites();
		this.secretsGenerator.remove();

		//Create response and put it into context
		DeleteServiceInstanceResponse deletionResponse = new DeleteServiceInstanceResponse();
		deletionResponse.withAsync(false);
		ctx.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE, deletionResponse);

		//Generate commit message and put it into context
		String msg = "Cassandra broker" + ": "+ CassandraProcessorConstants.OSB_OPERATION_DELETE + " instance id=" + serviceInstanceId;
		ctx.contextKeys.put(GitProcessorContext.commitMessage.toString(), msg);

	}



}

