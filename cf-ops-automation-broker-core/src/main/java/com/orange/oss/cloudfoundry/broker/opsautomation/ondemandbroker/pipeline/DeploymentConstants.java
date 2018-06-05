package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class DeploymentConstants {
    public static final String COAB = "coab";
    public static final String UNDERSCORE = "_";
    public static final String HYPHEN = "-";
    public static final String YML_EXTENSION = ".yml";
    public static final String DEPLOYMENT_DEPENDENCIES_FILENAME = "deployment-dependencies.yml";
    public static final String ENABLE_DEPLOYMENT_FILENAME = "enable-deployment.yml";
    public static final String DEPLOYMENT = "deployment";
    public static final String DEPLOYMENT_NAME_PATTERN = "@deployment_name@";
    public static final String GENERATION_EXCEPTION = "Generation fails";
    public static final String ROOT_DEPLOYMENT_EXCEPTION = "Root deployment directory doesn't exist";
    public static final String MODEL_DEPLOYMENT_EXCEPTION = "Model deployment directory doesn't exist";
    public static final String TEMPLATE_EXCEPTION = "Template directory doesn't exist";
    public static final String OPERATORS_EXCEPTION = "Operators directory doesn't exist";
    public static final String MANIFEST_FILE_EXCEPTION = "Model manifest file doesn't exist";
    public static final String VARS_FILE_EXCEPTION = "Model vars file doesn't exist";
    public static final String COAB_OPERATORS_FILE_EXCEPTION = "Coab operators file doesn't exist";
    public static final String SECRETS_EXCEPTION = "Secrets directory doesn't exist";;
    public static final String META_FILE_EXCEPTION = "Model meta file doesn't exist";
    public static final String SECRETS_FILE_EXCEPTION = "Model secrets file doesn't exist";
    public static final String OSB_OPERATION_CREATE = "create";
    public static final String OSB_OPERATION_DELETE = "delete";
    public static final String OSB_CREATE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest";
    public static final String OSB_DELETE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest";
}
