package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 *
 * Note: would deserve merging with CloudFlareConfig in the future
 */
@ConfigurationProperties(prefix = "cloudflare")
public class CloudFlareProperties {
    private int maxExecutionDurationSeconds = 600;
    private String routeSuffix;
    private String pathTFSpecs;
    private String filePrefix="cloudflare-instance-";
    private String pathToTfState;

    public int getMaxExecutionDurationSeconds() {
        return maxExecutionDurationSeconds;
    }

    public void setMaxExecutionDurationSeconds(int maxExecutionDurationSeconds) {
        this.maxExecutionDurationSeconds = maxExecutionDurationSeconds;
    }

    public String getRouteSuffix() {
        return routeSuffix;
    }

    public void setRouteSuffix(String routeSuffix) {
        this.routeSuffix = routeSuffix;
    }

    /**
     * path to tf configs e.g. "cloudflare-depls/terraform-config/spec"
     */
    public String getPathTFSpecs() {
        return pathTFSpecs;
    }

    public void setPathTFSpecs(String pathTFSpecs) {
        this.pathTFSpecs = pathTFSpecs;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    /**
     * e.g. cloudflare-depls/terraform-config/terraform.tfstate
     */
    public String getPathToTfState() {
        return pathToTfState;
    }

    public void setPathToTfState(String pathToTfState) {
        this.pathToTfState = pathToTfState;
    }
}
