package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class CassandraProcessorConstants {
    public static final String SERVICE_INSTANCE_PREFIX_DIRECTORY = "cassandra_";
    public static final String SECRETS_DIRECTORY = "secrets";
    public final static String ROOT_DEPLOYMENT_DIRECTORY = "coab-depls";
    public final static String MODEL_DEPLOYMENT_DIRECTORY = "cassandra";
    public static final String META_FILENAME = "meta.yml";
    public static final String META_CONTENT = "meta: {}";
    public static final String SECRETS_FILENAME = "secrets.yml";
    public static final String SECRETS_CONTENT = "secrets: {}";
    public static final String ENABLE_DEPLOYMENT_FILENAME = "enable-deployment.yml";
    public static final String ENABLE_DEPLOYMENT_CONTENT = "---";
    public static final String ROOT_DEPLOYMENT_EXCEPTION = "Root deployment doesn't exist";
    public static final String MODEL_DEPLOYMENT_EXCEPTION = "Model deployment doesn't exist";
}
