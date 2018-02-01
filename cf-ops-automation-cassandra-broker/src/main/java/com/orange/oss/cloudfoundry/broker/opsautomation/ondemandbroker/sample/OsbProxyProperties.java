package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "pipeline")
public class OsbProxyProperties {
    private int maxExecutionDurationSeconds = 600;
    private String osbDelegateUser;
    private String osbDelegatePassword;

    public int getMaxExecutionDurationSeconds() {
        return maxExecutionDurationSeconds;
    }

    public void setMaxExecutionDurationSeconds(int maxExecutionDurationSeconds) {
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
}
