package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class CassandraProcessorConstants {
    public static final String SERVICE_INSTANCE_PREFIX_DIRECTORY = "cassandra_";
    public static final String BROKER_PREFIX = "cassandra-broker_";
    public static final String SECRETS_DIRECTORY = "secrets";
    public static final String TEMPLATE_DIRECTORY = "template";
    public static final String ROOT_DEPLOYMENT_DIRECTORY = "coab-depls";
    public static final String MODEL_DEPLOYMENT_DIRECTORY = "cassandra";
    public static final String MODEL_MANIFEST_FILENAME = "cassandra-tpl.yml";
    public static final String MODEL_VARS_FILENAME = "cassandra-vars-tpl.yml";
    public static final String MANIFEST_FILENAME_SUFFIX = "-tpl.yml";
    public static final String VARS_FILENAME_SUFFIX = "-vars-tpl.yml";
    public static final String META_FILENAME = "meta.yml";
    public static final String META_CONTENT = "meta: {}";
    public static final String SECRETS_FILENAME = "secrets.yml";
    public static final String SECRETS_CONTENT = "secrets: {}";
    public static final String ENABLE_DEPLOYMENT_FILENAME = "enable-deployment.yml";
    public static final String ENABLE_DEPLOYMENT_CONTENT = "---";
    public static final String ROOT_DEPLOYMENT_EXCEPTION = "Root deployment directory doesn't exist";
    public static final String MODEL_DEPLOYMENT_EXCEPTION = "Model deployment directory doesn't exist";
    public static final String TEMPLATE_EXCEPTION = "Template directory doesn't exist";
    public static final String MANIFEST_FILE_EXCEPTION = "Model manifest file doesn't exist";
    public static final String VARS_FILE_EXCEPTION = "Model vars file doesn't exist";
    public static final String GENERATION_EXCEPTION = "Generation fails";
    public static final String REMOVAL_EXCEPTION = "Removal fails";
    public static final String DEPLOYMENT_DEPENDENCIES_FILENAME = "deployment-dependencies.yml";
    public static final String OPERATORS_FILENAME = "coab-operators.yml";
    public static final String SERVICE_INSTANCE_PATTERN = "@service_instance@";
    public static final String URL_PATTERN = "@url@";
    public static final String YML_SUFFIX = ".yml";
    public static final String OSB_OPERATION_CREATE = "create";
    public static final String OSB_OPERATION_DELETE = "delete";
    public static final String OSB_OPERATION_UPDATE = "update";
    public static final String OSB_CREATE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest";
    public static final String OSB_DELETE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest";
    public static final String OSB_UPDATE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.UpdateServiceInstanceRequest";


}
