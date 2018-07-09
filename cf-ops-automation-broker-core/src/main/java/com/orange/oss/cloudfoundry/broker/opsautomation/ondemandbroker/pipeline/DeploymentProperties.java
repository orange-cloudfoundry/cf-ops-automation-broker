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
    private String secrets = "secrets"; //Secrets directory (i.e secrets)
    private String vars = "vars"; //Vars suffix (i.e vars)
    private String operators = "operators"; //Operators suffix (i.e operators)

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

    public String getSecrets() {
        return secrets;
    }

    public void setSecrets(String secrets) {
        this.secrets = secrets;
    }

    public String getVars() {
        return vars;
    }

    public void setVars(String vars) {
        this.vars = vars;
    }

    public String getOperators() {
        return operators;
    }

    public void setOperators(String operators) {
        this.operators = operators;
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
}

