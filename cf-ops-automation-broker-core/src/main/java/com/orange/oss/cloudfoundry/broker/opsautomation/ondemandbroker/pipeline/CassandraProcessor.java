package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class CassandraProcessor extends DefaultBrokerProcessor {

	private static Logger logger = LoggerFactory.getLogger(CassandraProcessor.class.getName());
	private final static String ROOT_DEPLOYMENT = "coab-depls";


	@Override
	public void preCreate(Context ctx) {
		//Need to retrieve workDir context
		Path workDir = (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
		logger.debug("workDir is " + workDir.toString());

		//Need to retrieve service instance id from  context
		Map<String, Object> contextKeys = ctx.contextKeys;
		CreateServiceInstanceRequest request = (CreateServiceInstanceRequest) contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST);
		String serviceInstanceId = request.getServiceInstanceId();
		logger.debug("service instance id is " + serviceInstanceId);

		//Check pre-requisites
		//this.checkPrequisites(workDir);
		
		
		//Generate expected Cassandra paas-template structure
		//this.generateCassandraTemplatesStructure(workDir, serviceInstanceId);

		//Generate expected Cassandra secrets structure
		this.generateCassandraSecretsStructure(workDir, serviceInstanceId);

	}

	private void checkPrequisites(Path workDir) {		
	}

	private void generateCassandraSecretsStructure(Path workDir, String serviceInstanceId) {
		File deploymentDirectory=new File(String.valueOf(workDir)+"/"+ ROOT_DEPLOYMENT +"/"+ "cassandra_" + serviceInstanceId);
		File deploymentSecretsDirectory=new File(String.valueOf(workDir)+"/"+ ROOT_DEPLOYMENT +"/"+ "cassandra_" + serviceInstanceId+"/" + "secrets");
		deploymentDirectory.mkdir();
		deploymentSecretsDirectory.mkdir();
	}

	private void generateCassandraTemplatesStructure(Path workDir, String serviceInstanceId) {
	}


}	

