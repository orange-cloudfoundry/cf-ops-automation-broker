package com.orange.oss.bosh.deployer.manifest;


import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Manifest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;



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
		
		ManifestComposer composer=new ManifestComposer(new ManifestParser());
		
		Manifest manifest=composer.composeBoshManifest(spec);
		
		
		
	}
	
	
	

}
