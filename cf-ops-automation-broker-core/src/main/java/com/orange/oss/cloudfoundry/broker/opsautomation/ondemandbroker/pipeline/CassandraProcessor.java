package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;

import java.nio.file.Path;
import java.time.Clock;

public class CassandraProcessor extends DefaultBrokerProcessor {

	private static Logger logger = LoggerFactory.getLogger(CassandraProcessor.class.getName());

	private String templatesRepositoryAliasName;
	private String secretsRepositoryAliasName;
	private PipelineCompletionTracker tracker;


	public CassandraProcessor(String templatesRepositoryAliasName, String secretsRepositoryAliasName, Clock clock) {
		this.templatesRepositoryAliasName = templatesRepositoryAliasName;
		this.secretsRepositoryAliasName = secretsRepositoryAliasName;
		tracker = new PipelineCompletionTracker(clock);
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
		TemplatesGenerator templates = new TemplatesGenerator(templatesWorkDir, serviceInstanceId);
		templates.checkPrerequisites();
		templates.generate();

		//Check pre-requisites and generate paas-secrets structure
		SecretsGenerator secrets = new SecretsGenerator(secretsWorkDir, serviceInstanceId);
		secrets.checkPrerequisites();
		secrets.generate();

		//Create response and put it into context
		CreateServiceInstanceResponse creationResponse = new CreateServiceInstanceResponse();
		creationResponse.withAsync(true);
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
		GetLastServiceOperationResponse operationResponse = tracker.getDeploymentExecStatus(secretsWorkDir, operationRequest.getServiceInstanceId(), operationRequest.getOperation());

		//Put the response into context
		ctx.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE, operationResponse);
	}


}

