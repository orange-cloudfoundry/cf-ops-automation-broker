package com.orange.oss.bosh.deployer.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.orange.oss.bosh.deployer.manifest.ManifestMapping.InstanceGroup;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Job;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Manifest;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Network;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Release;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Stemcell;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Update;


/**
 * Generates a deployment manifest from  compact parameters, and adapted default
 * - same stemcell, from cloud config
 * - latest releases
 * - expecting bosh 2 links autowiring between jobs of the deployment
 * - different strategies tbd (ie: public ip, ip exposed with route registrar, max in flight, consul dns registration)
 * 
 * 
 * @author poblin-orange
 *
 */

@Component
public class ManifestComposer {

	private static Logger logger=LoggerFactory.getLogger(ManifestComposer.class.getName());	
	
	private static final String vmType = "default";
	private static final String netName = "net-bosh-ondemand";
	private static final String stemcellOs = "ubuntu-trusty";
	private static final String stemcellAlias = "trusty";

	
	@Autowired
	ManifestParser manifestParser;
	
	
	public Manifest composeBoshManifest(DeploymentSpec spec){
		
		String uuid="xx"; //FIXME: no need for UUID in manifest with bosh2
		String deploymentName=spec.deploymentNamePrefix+"-"+UUID.randomUUID();
		
		
		Manifest manifest=new Manifest();
		manifest.director_uuid=uuid;
		manifest.name=deploymentName;
		manifest.update=new Update();
		manifest.update.serial=true;
		manifest.update.max_in_flight=1;
		
		manifest.update.canaries=1;
		manifest.update.canary_watch_time="30000-240000";
		manifest.update.update_watch_time="30000-240000";
		
		
		//stemcells
		manifest.stemcells=new ArrayList<Stemcell>();
		ManifestMapping.Stemcell stemcell=new Stemcell();
		stemcell.alias=stemcellAlias;
		stemcell.os=stemcellOs;
		stemcell.version="latest";
		manifest.stemcells.add(stemcell);
		
		
		
		//network and az for instance groups
		List<Network> hzNetWorks=new ArrayList<Network>();
		Network net=new Network();
		net.name=netName;
		hzNetWorks.add(net);
		
		List<String> azs=new ArrayList<String>();
		azs.add("z1");
			
		
		//instances group
		manifest.instance_groups=new ArrayList<>();
		
		Set<String> uniqueReleases=new HashSet<String>();
		
		
		for (DeploymentSpec.InstanceGroup dsig:spec.instanceGroups)
		{
		InstanceGroup instanceIG=new InstanceGroup();
		instanceIG.instances=dsig.instances;
		instanceIG.azs=azs;
		instanceIG.name=dsig.jobName;
		
		instanceIG.networks=hzNetWorks;
		instanceIG.stemcell=stemcellAlias;
		instanceIG.vm_type=vmType;
		
		
		Job instanceJob=new Job();
		instanceJob.name=dsig.jobName;
		instanceJob.release=dsig.releaseName;
		instanceIG.jobs=new ArrayList<Job>();
		instanceIG.jobs.add(instanceJob);
		
		uniqueReleases.add(dsig.releaseName);
		
		
		Map properties=new HashMap<String,String>();
		properties.putAll(dsig.properties);
		
		//generate random props
		for (String randomProp:dsig.randomCredentials){
			
			String generatedPassword="(("+randomProp +"))"; //uses credhub to generate password
			properties.put(randomProp, generatedPassword);
		}
		
		//generate Json/Yaml map structure from flat properties
		instanceJob.properties=PropertyMapper.map(properties);
		manifest.instance_groups.add(instanceIG);
		}
		
		
		//releases
		//add distinct all release
		
		for (String uniqueReleaseName: uniqueReleases){
			Release r=new Release();
			r.name=uniqueReleaseName;
			r.version="latest";
			manifest.releases.add(r);
		}
		
		
		
		logger.info("composed manifest:\n{}",manifestParser.generate(manifest));
		return manifest;

	}
	
	
}
