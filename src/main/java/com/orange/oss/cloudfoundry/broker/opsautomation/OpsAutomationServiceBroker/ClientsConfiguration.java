package com.orange.oss.cloudfoundry.broker.opsautomation.OpsAutomationServiceBroker;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConfigProps.class)
public class ClientsConfiguration {

	
	@Bean
	ConcourseClient concourseClient(ConfigProps props){
		return new ConcourseClient();
	}
	
	@Bean
	UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider(ConfigProps props){
		return new UsernamePasswordCredentialsProvider(props.getGitUser(),props.getGitPassword());
	}
	
	@Bean
	GitClient gitClient(UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider,ConfigProps props ){
		return new GitClient(props.getGitBaseUrl(),usernamePasswordCredentialsProvider);
	}	
}
