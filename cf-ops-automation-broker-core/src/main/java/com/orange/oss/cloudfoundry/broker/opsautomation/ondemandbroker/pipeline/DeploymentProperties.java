package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "deployment")
public class DeploymentProperties {

    private String brokerDisplayName = "Cassandra"; //used in broker traces.
    private String rootDeployment = "coab-depls"; //Root deployment (i.e coab-depls)
    private String modelDeployment = "cassandravarsops"; //Model deployment (i.e cassandra, cassandravarsops, ...)
    private String modelDeploymentShortAlias = "c"; //shortname for the model deployment. Enables distinguishing services. Should be short so that broker URL remains shorter than 63 chars
    private String modelDeploymentSeparator = DeploymentConstants.UNDERSCORE; //shortname for the model deployment. Enables distinguishing services. Should be short so that broker URL remains shorter than 63 chars
    private String dashboardUrlTemplate = null; // dashboard url template e.g. "https://shield_{0}.redacted-ops-domain.com" where {0} resolves to service instance guid.

    public String getRootDeployment() {
        return rootDeployment;
    }

    public void setRootDeployment(String rootDeployment) {
        this.rootDeployment = rootDeployment;
    }

    public String getModelDeployment() {
        return modelDeployment;
    }

    public void setModelDeployment(String modelDeployment) {
        this.modelDeployment = modelDeployment;
    }

    public String getModelDeploymentShortAlias() {
        return modelDeploymentShortAlias;
    }

    public void setModelDeploymentShortAlias(String modelDeploymentShortAlias) {
        this.modelDeploymentShortAlias = modelDeploymentShortAlias;
    }

    public String getBrokerDisplayName() {
        return brokerDisplayName;
    }

    public void setBrokerDisplayName(String brokerDisplayName) {
        this.brokerDisplayName = brokerDisplayName;
    }

    public String getDashboardUrlTemplate() { return dashboardUrlTemplate; }
    public void setDashboardUrlTemplate(String dashboardUrlTemplate) { this.dashboardUrlTemplate = dashboardUrlTemplate; }

    public String getModelDeploymentSeparator() { return modelDeploymentSeparator; }

    public void setModelDeploymentSeparator(String modelDeploymentSeparator) { this.modelDeploymentSeparator = modelDeploymentSeparator; }


}

