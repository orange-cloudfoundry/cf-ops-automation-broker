package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import java.io.IOException;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitServer.NO_OP_INITIALIZER;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.BoshServiceProvisionningTest.initPaasSecret;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.BoshServiceProvisionningTest.initPaasTemplate;

@Configuration
@Order(value= Ordered.HIGHEST_PRECEDENCE)
public class HermeticGitServerTestConfiguration  {

	@Bean
	public GitServer gitServer(DeploymentProperties deploymentProperties) throws IOException {
		GitServer gitServer = new GitServer();

		gitServer.startEphemeralReposServer(NO_OP_INITIALIZER);
		gitServer.initRepo("paas-template.git", git -> initPaasTemplate(git, deploymentProperties));
		gitServer.initRepo("paas-secrets.git", git -> initPaasSecret(git, deploymentProperties));

		return gitServer;
	}
}
