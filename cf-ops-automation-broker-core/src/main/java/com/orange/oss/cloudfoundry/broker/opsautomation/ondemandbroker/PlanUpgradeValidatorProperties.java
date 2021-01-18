package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "plans.upgrade")
public class PlanUpgradeValidatorProperties {

    private Map<String, List<String>> supportedFromTo = new HashMap<>();

    private String rejectedMessageTemplate = "plan upgrade from {0} to {1} are not supported, please prefer creating a new instance and export/import your data into it. List of supported plan upgrades from {0} is: {2}";

    public Map<String, List<String>> getSupportedFromTo() {
        return supportedFromTo;
    }

    public void setSupportedFromTo(Map<String, List<String>> supportedFromTo) {
        this.supportedFromTo = supportedFromTo;
    }

    public String getRejectedMessageTemplate() {
        return rejectedMessageTemplate;
    }

    public void setRejectedMessageTemplate(String rejectedMessageTemplate) {
        this.rejectedMessageTemplate = rejectedMessageTemplate;
    }

}
