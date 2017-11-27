package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "git")
public class GitProperties {

	private String user;
	private String password;
	private String url;
	private String committerName = "coab";
	private String committerEmail = "coab@orange.com";


	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}


	public String committerName() {
		return committerName;
	}
	public void setCommitterName(String committerName) { this.committerName = committerName; }

	public String committerEmail() {
		return committerEmail;
	}
	public void setCommitterEmail(String committerEmail) { this.committerEmail = committerEmail; }
}

