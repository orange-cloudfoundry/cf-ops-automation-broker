package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "credhub")
public class CredHubProperties {

    private String url;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
}

