package com.orange.oss.bosh.deployer.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.orange.oss.bosh.deployer.manifest.DeploymentSpec.InstanceGroup;

@Component
public class DeploymentSpecFactory {

	/**
	 * gets a deployment spec factory
	 * @return
	 */
	public DeploymentSpec spec(){
		DeploymentSpec spec=new DeploymentSpec();
		
		
		//FIXME: generate spec from properties ?
		spec.instanceGroups=new ArrayList<InstanceGroup>();
		
		InstanceGroup hz=new InstanceGroup();
		hz.instances=2;
		hz.jobName="hazelcast_node";
		hz.releaseName="hazelcast";
		hz.exposeAsServiceInstanceIp=true;
		
		Map<String,String> properties=new HashMap<String,String>();
		properties.put("hazelcast.jvm.memoryMo", "3000");
		properties.put("hazelcast.group.name", "hz-group");
		hz.properties.putAll(properties);
		
		hz.randomCredentials.add("hazelcast.group.password");
		
		spec.instanceGroups.add(hz);
		
		InstanceGroup mancenter=new InstanceGroup();
		mancenter.instances=1;
		mancenter.jobName="hazelcast_mancenter";
		mancenter.releaseName="hazelcast";
		mancenter.exposeAsDashBoard=true;
		mancenter.exposeWithRouteRegistrar=true;
		
		spec.instanceGroups.add(mancenter);		
		
		
		return spec;
	}
}
