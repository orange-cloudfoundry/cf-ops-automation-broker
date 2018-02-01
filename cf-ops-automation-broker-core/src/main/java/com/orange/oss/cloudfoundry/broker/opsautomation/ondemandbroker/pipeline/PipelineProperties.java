package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "git.paas-template")
public class PipelineProperties {

    /**
     * Which branch to commit to
     */
    private String createBranchIfMissing;

    /**
     * Base branch from which to create branch above
     */
    private String checkOutRemoteBranch;

    public String getCreateBranchIfMissing() {
        return createBranchIfMissing;
    }

    public void setCreateBranchIfMissing(String createBranchIfMissing) {
        this.createBranchIfMissing = createBranchIfMissing;
    }

    public String getCheckOutRemoteBranch() {
        return checkOutRemoteBranch;
    }

    public void setCheckOutRemoteBranch(String checkOutRemoteBranch) {
        this.checkOutRemoteBranch = checkOutRemoteBranch;
    }
}
