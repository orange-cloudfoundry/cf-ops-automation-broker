package com.orange.oss.bosh.deployer.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.bosh.deployer.manifest.ManifestMapping.InstanceGroup;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Job;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Manifest;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Network;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Release;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Stemcell;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Update;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("integration")

public class BoshClientDeployComposedManifestTest {

	@Autowired
	ManifestParser manifestParser;
	
	
	@Autowired
	DeploymentSpecFactory specFactory;
	
	
	@Autowired
	ManifestComposer manifestComposer;
	
	
	
	private static Logger logger=LoggerFactory.getLogger(BoshClientDeployComposedManifestTest.class.getName());
	
	@Test
	public void testDeployGeneratedComposedManifest() {
	
		DeploymentSpec spec=this.specFactory.spec();
		spec.deploymentNamePrefix="generated-hazelcast";
		Manifest m=this.manifestComposer.composeBoshManifest(spec);
		
		logger.info("composed manifest:\n{}",manifestParser.generate(m));
		
	}
	
	@Test
	public void testDeployComposedManifest() {

		String deploymentName="composed-hazelcast";
		
		Manifest manifest=new Manifest();
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
		stemcell.alias="trusty";
		stemcell.os="ubuntu-trusty";
		stemcell.version="latest";
		manifest.stemcells.add(stemcell);
		
		
		//releases
		manifest.releases=new ArrayList<Release>();
		ManifestMapping.Release release=new Release();
		release.name="hazelcast";
		release.version="latest";
		manifest.releases.add(release);
		
		
		//network and az for instance groups
		List<Network> hzNetWorks=new ArrayList<Network>();
		Network net=new Network();
		net.name="net-bosh-ondemand";
		hzNetWorks.add(net);
		
		List<String> azs=new ArrayList<String>();
		azs.add("z1");
			
		
		//instances group
		manifest.instance_groups=new ArrayList<>();
		
		{
		InstanceGroup instanceIG=new InstanceGroup();
		instanceIG.instances=2;
		instanceIG.azs=azs;
		instanceIG.name="hazelcast-instances";
		
		instanceIG.networks=hzNetWorks;
		instanceIG.stemcell="trusty";
		instanceIG.vm_type="default";
		
		
		Job instanceJob=new Job();
		instanceJob.name="hazelcast_node";
		instanceJob.release="hazelcast";
		instanceIG.jobs=new ArrayList<Job>();
		instanceIG.jobs.add(instanceJob);
		
		
		Map properties=new HashMap<String,String>();
		properties.put("hazelcast.jvm.memoryMo", "3000");
		properties.put("hazelcast.group.name", "hz-group");
		properties.put("hazelcast.group.password", "eentepAxHo");
		
		//generate Json/Yaml map structure from flat properties
		instanceJob.properties=PropertyMapper.map(properties);
		
		
		
		
				

		manifest.instance_groups.add(instanceIG);
		}
		
		{
		InstanceGroup managerIG=new InstanceGroup();
		managerIG.instances=1;
		managerIG.azs=azs;
		managerIG.name="manager";
		
		managerIG.networks=hzNetWorks;
		managerIG.stemcell="trusty";
		managerIG.vm_type="default";

		Job managerJob=new Job();
		managerJob.name="hazelcast_mancenter";
		managerJob.release="hazelcast";
		managerIG.jobs=new ArrayList<Job>();
		managerIG.jobs.add(managerJob);
		
		manifest.instance_groups.add(managerIG);
		}
		
		logger.info("composed manifest:\n{}",manifestParser.generate(manifest));
		
	}

}
