package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "deployment")
@Validated
public class DeploymentProperties {

    /**
     * used in broker traces.
     */
    private String brokerDisplayName = "Cassandra";

    /**
     * Root deployment (i.e coab-depls)
     */
    private String rootDeployment = "coab-depls";

    /**
     * Model deployment (i.e cassandra, cassandravarsops, ...)
     */
    private String modelDeployment = "cassandravarsops";

    /**
     * shortname for the model deployment. Enables distinguishing services. Should be short so that broker URL remains shorter than 63 chars
     */
    private String modelDeploymentShortAlias = "c";

    /**
     * separator character for deployment model string. Default (_) can't be used in FQDNs
     */
    private String modelDeploymentSeparator = DeploymentConstants.UNDERSCORE;

    /**
     * dashboard url template e.g. "https://shield_{0}.redacted-ops-domain.com" where {0} resolves to service instance guid.
     */
    private String dashboardUrlTemplate = null;

    /**
     * When set to true, then service instance operations (create/update/delete) are rejected
     */
    private boolean serviceInstanceReadOnlyMode= false;

    public static final String DEFAULT_READ_ONLY_MESSAGE = "Maintenance mode in progress. Service instance operations " +
        "(create-service, update-service, delete-service) are not available. Service binding operations are available" +
        " (bind-service unbind-service)";

    /**
     * User facing message to display when a service instance operation is requested while
     * {@link #serviceInstanceReadOnlyMode} is set to true
     */
    @NotNull
    private String serviceInstanceReadOnlyMessage = DEFAULT_READ_ONLY_MESSAGE;

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

    public boolean isServiceInstanceReadOnlyMode() {
        return serviceInstanceReadOnlyMode;
    }

    public void setServiceInstanceReadOnlyMode(boolean serviceInstanceReadOnlyMode) {
        this.serviceInstanceReadOnlyMode = serviceInstanceReadOnlyMode;
    }

    public String getServiceInstanceReadOnlyMessage() {
        return serviceInstanceReadOnlyMessage;
    }

    public void setServiceInstanceReadOnlyMessage(String serviceInstanceReadOnlyMessage) {
        this.serviceInstanceReadOnlyMessage = serviceInstanceReadOnlyMessage;
    }

}

