package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import com.orange.oss.bosh.deployer.manifest.ManifestMapping;
import com.orange.oss.bosh.deployer.manifest.ManifestParser;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitMediationContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediation;

/**
 * Mediation relative to cf-ops-automation pipeline file generation
 * @author poblin-orange
 *
 */
public class GitPipelineTemplatingMediation extends DefaultBrokerMediation {

	private static Logger logger=LoggerFactory.getLogger(GitPipelineTemplatingMediation.class.getName());

	private String baseDir;
    private Resource manifestResource;
	
    /**
     * contructor
     * @param baseDir: base directory inside the git repo to generate the manifest (cf-ops-automation naming convention, maps to bosh director)
     * @param manifestTemplateResource: a resource for a yaml bosh manifest template
     */
	public GitPipelineTemplatingMediation(String baseDir,Resource manifestTemplateResource) {
		super();
		this.manifestResource=manifestTemplateResource;
		this.baseDir=baseDir;
	}
	
	
	
	@Override
	public void preCreate(Context ctx) {
		//generate pipeline files. need to retrieve context.
		Path workDir= (Path) ctx.contextKeys.get(GitMediationContext.workDir.toString());
		
		//TODO generate the manifest
		logger.info("generating files for pipelines");
		
		ManifestParser parser=new ManifestParser();
		ManifestMapping.Manifest m;
		try {
			m = parser.parser(new String(Files.readAllBytes(manifestResource
					.getFile()
					.toPath())));
			logger.info("parsed manifest"+m );
			
			
			
			logger.info("create manifest file in workDir");
			String deploymentName="hazelcast";
			
			m.name=deploymentName; //patch deployment Name
			
			String patchedManifest=parser.generate(m);
			File directory = new File(String.valueOf(workDir));
			File deploymentBaseDirectory=new File(String.valueOf(workDir)+"/"+this.baseDir);
			File deploymentDirectory=new File(String.valueOf(workDir)+"/"+this.baseDir+"/"+deploymentName);
			File deploymentTemplateDirectory=new File(String.valueOf(workDir)+"/"+this.baseDir+"/"+deploymentName+"/template");			
			
		    if (!deploymentDirectory.exists()) {
		    	deploymentDirectory.mkdir();
		    }
		    
		    if (!deploymentBaseDirectory.exists()) {
		    	deploymentBaseDirectory.mkdir();
		    }
		    
		    
		    deploymentTemplateDirectory.mkdir();
		    
		    
		    Path path = Paths.get(directory.getAbsolutePath()+"/"
	    			+deploymentName
					+"/template/"+deploymentName+".yml");
		    
			Files.write(path,
		    		patchedManifest.getBytes());
			
		    
			logger.info("pipeline file successfully generated");		    
		    
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		


		
		
		
	}


	
	
	

}
