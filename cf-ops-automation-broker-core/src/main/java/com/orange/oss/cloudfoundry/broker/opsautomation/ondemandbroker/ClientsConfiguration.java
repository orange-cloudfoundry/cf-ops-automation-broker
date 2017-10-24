package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConfigProps.class)
public class ClientsConfiguration {

	
	@Bean
	@ConditionalOnMissingBean	
	ConcourseClient concourseClient(ConfigProps props){
		return new ConcourseClient();
	}
	
	@Bean
	@ConditionalOnMissingBean	
	UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider(ConfigProps props){
		return new UsernamePasswordCredentialsProvider(props.getGitUser(),props.getGitPassword());
	}
	
	
}
