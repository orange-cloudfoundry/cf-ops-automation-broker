package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * deployment Strategy for bosh manifests files
 * @author poblin-orange
 *
 */
@Component
@Order(1000)
public class BoshManifestTemplatingStrategy implements DeploymentTemplate {

	
	/* (non-Javadoc)
	 * @see com.orange.oss.cloudfoundry.broker.opsautomation.OpsAutomationServiceBroker.DeploymentTemplate#createDeploymentTemplate()
	 */
	@Override
	public void createDeploymentTemplate(){
		
	}
}
