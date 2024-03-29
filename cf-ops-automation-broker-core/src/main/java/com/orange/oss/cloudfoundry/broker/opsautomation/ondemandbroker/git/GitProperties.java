package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "git")
@Validated
public class GitProperties {

	private String user;
	private String password;
	private String url;
	private String committerName = "coab";
	private String committerEmail = "coab@orange.com";
	private boolean usePooling = true;
	@NestedConfigurationProperty
	private RetryProperties retry = new RetryProperties();
	@NestedConfigurationProperty
	private PoolingProperties poolingProperties = new PoolingProperties();

	/**
	 * Where submodules are replicated by default (e.g. "https://gitlab.internal.paas/"
	 * Unit tests use "git://127.0.0.1:9418/" by default.
	 */
	private String replicatedSubModuleBasePath;


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

	public void setReplicatedSubModuleBasePath(String replicatedSubModuleBasePath) { this.replicatedSubModuleBasePath = replicatedSubModuleBasePath; }
	public String getReplicatedSubModuleBasePath() { return replicatedSubModuleBasePath; }

	public boolean isUsePooling() { return usePooling; }
	public void setUsePooling(boolean usePooling) { this.usePooling = usePooling; }

	public RetryProperties getRetry() { return retry; }
	public void setRetry(RetryProperties retry) { this.retry = retry; }

	public PoolingProperties getPoolingProperties() {return poolingProperties;}
	public void setPoolingProperties(PoolingProperties poolingProperties) {this.poolingProperties = poolingProperties;}

}

