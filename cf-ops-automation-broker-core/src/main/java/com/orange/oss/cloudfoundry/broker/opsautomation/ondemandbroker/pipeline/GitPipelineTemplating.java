package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitMediationContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediation;

/**
 * Mediation relative to cf-ops-automation pipeline file generation
 * @author poblin-orange
 *
 */
public class GitPipelineTemplating extends DefaultBrokerMediation {

	private static Logger logger=LoggerFactory.getLogger(GitPipelineTemplating.class.getName());
	
	@Override
	public void preCreate(Context ctx) {
		//generate pipeline files. need to retrieve context.
		Path workDir= (Path) ctx.contextKeys.get(GitMediationContext.workDir.toString());
		
		//TODO generate the manifest
		logger.info("generating files for pipelines");
		
		
	}


	
	
	

}
