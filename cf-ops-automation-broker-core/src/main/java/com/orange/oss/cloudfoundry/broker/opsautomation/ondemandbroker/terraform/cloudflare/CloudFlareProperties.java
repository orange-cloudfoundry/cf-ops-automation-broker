package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 *
 * Note: would deserve merging with CloudFlareConfig in the future
 */
@ConfigurationProperties(prefix = "cloudflare")
@Validated
public class CloudFlareProperties {
    @Min(1)
    private int maxExecutionDurationSeconds = 600;
    @NotNull
    private String routeSuffix;
    @NotNull
    private String pathTFSpecs;
    @NotNull
    private String filePrefix="cloudflare-instance-";
    @NotNull
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
