package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix="broker",ignoreUnknownFields=false)
public class ConfigProps {

	String gitBaseUrl;
	String gitUser;
	String gitPassword;
	
	String concourseBaseUrl;
	String concourseUser;
	String concoursePassword;
	
	public String getGitBaseUrl() {
		return gitBaseUrl;
	}
	public void setGitBaseUrl(String gitBaseUrl) {
		this.gitBaseUrl = gitBaseUrl;
	}
	public String getGitUser() {
		return gitUser;
	}
	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}
	public String getGitPassword() {
		return gitPassword;
	}
	public void setGitPassword(String gitPassword) {
		this.gitPassword = gitPassword;
	}
	public String getConcourseBaseUrl() {
		return concourseBaseUrl;
	}
	public void setConcourseBaseUrl(String concourseBaseUrl) {
		this.concourseBaseUrl = concourseBaseUrl;
	}
	public String getConcourseUser() {
		return concourseUser;
	}
	public void setConcourseUser(String concourseUser) {
		this.concourseUser = concourseUser;
	}
	public String getConcoursePassword() {
		return concoursePassword;
	}
	public void setConcoursePassword(String concoursePassword) {
		this.concoursePassword = concoursePassword;
	}
	
}
