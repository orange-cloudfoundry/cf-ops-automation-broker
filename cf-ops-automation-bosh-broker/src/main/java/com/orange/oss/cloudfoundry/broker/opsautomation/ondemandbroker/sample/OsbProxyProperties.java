package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "pipeline")
public class OsbProxyProperties {
    private long maxExecutionDurationSeconds = 1200L;
    private String osbDelegateUser;
    private String osbDelegatePassword;

    private String brokerUrlPattern;

    /**
     * When true, then the inner broker will not receive unprovision calls (i.e. upon `cf delete-service-instance`)
     * This is designed to support undeletes by operators until they explicitly approve the deletion the associated
     * COA deployment (and the associated underlying bosh deployment)
     */
    private boolean skipDeProvision = false;

    public String getBrokerUrlPattern() {
        return brokerUrlPattern;
    }

    public void setBrokerUrlPattern(String brokerUrlPattern) {
        this.brokerUrlPattern = brokerUrlPattern;
    }

    public long getMaxExecutionDurationSeconds() {
        return maxExecutionDurationSeconds;
    }

    public void setMaxExecutionDurationSeconds(long maxExecutionDurationSeconds) {
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;
    }

    public String getOsbDelegateUser() {
        return osbDelegateUser;
    }

    public void setOsbDelegateUser(String osbDelegateUser) {
        this.osbDelegateUser = osbDelegateUser;
    }

    public String getOsbDelegatePassword() {
        return osbDelegatePassword;
    }

    public void setOsbDelegatePassword(String osbDelegatePassword) {
        this.osbDelegatePassword = osbDelegatePassword;
    }

    public boolean isSkipDeProvision() {
        return skipDeProvision;
    }

    public void setSkipDeProvision(boolean skipDeProvision) {
        this.skipDeProvision = skipDeProvision;
    }

}
