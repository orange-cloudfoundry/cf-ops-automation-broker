package com.orange.oss.bosh.deployer.manifest;



import org.fest.assertions.Assertions;
import org.junit.Test;

import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Manifest;



public class DeploymentSpecFactoryTest {

	
	
	@Test
	public void testSpec() {
		DeploymentSpecFactory factory=new DeploymentSpecFactory();
		DeploymentSpec spec=factory.spec();
		
		Assertions.assertThat(spec).isNotNull();
		Assertions.assertThat(spec.instanceGroups.size()).isEqualTo(2);
		
	}
	
	
	@Test
	public void testManifestFromDeploymentSpec(){

		DeploymentSpecFactory factory=new DeploymentSpecFactory();
		DeploymentSpec spec=factory.spec();
		
		ManifestComposer composer=new ManifestComposer();
		
		Manifest manifest=composer.composeBoshManifest(spec);
		
		
		
	}
	
	
	

}
