package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class DeploymentConstants {
    static final String COAB = "coab";
    static final String UNDERSCORE = "_";
    static final String HYPHEN = "-";
    public static final String YML_EXTENSION = ".yml";
    /**
     * <A href="https://github.com/orange-cloudfoundry/cf-ops-automation#ops-files">COA operators</A>
     * <blockquote>By convention, all files in template dir matching *-operators.yml are used by bosh-deployment as ops-files inputs.</blockquote>
     */
    static final String COA_OPERATORS_FILE_SUFFIX= HYPHEN + "operators" + YML_EXTENSION;
    static final String COA_TEMPLATE_FILE_SUFFIX= HYPHEN + "tpl" + YML_EXTENSION;
    static final String COA_VARS_FILE= HYPHEN + "vars";
    static final String DEPLOYMENT_DEPENDENCIES_FILENAME = "deployment-dependencies.yml";
    static final String ENABLE_DEPLOYMENT_FILENAME = "enable-deployment.yml";
    static final String DEPLOYMENT = "bosh";
    static final String TEMPLATE = "template";
    public static final String DEPLOYMENT_NAME_PATTERN = "@deployment_name@";
    static final String GENERATION_EXCEPTION = "Generation fails";
    static final String REMOVAL_EXCEPTION = "Removal fails";
    static final String SEARCH_EXCEPTION = "Search fails";
    static final String ROOT_DEPLOYMENT_EXCEPTION = "Root deployment directory doesn't exist at: ";
    static final String MODEL_DEPLOYMENT_EXCEPTION = "Model deployment directory doesn't exist at: ";
    static final String TEMPLATE_EXCEPTION = "Template directory doesn't exist at: ";
    static final String OPERATORS_EXCEPTION = "Operators directory doesn't exist at: ";
    static final String MANIFEST_FILE_EXCEPTION = "Model manifest file doesn't exist";
    static final String VARS_FILE_EXCEPTION = "Model vars file doesn't exist";
    static final String COAB_OPERATORS_FILE_EXCEPTION = "Coab operators file doesn't exist";
    static final String SECRETS_EXCEPTION = "Secrets directory doesn't exist at: ";
    static final String META_FILE_EXCEPTION = "Model meta file doesn't exist";
    static final String SECRETS_FILE_EXCEPTION = "Model secrets file doesn't exist";
    static final String OSB_OPERATION_CREATE = "create";
    public static final String OSB_OPERATION_DELETE = "delete";
    static final String OSB_CREATE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest";
    static final String OSB_DELETE_REQUEST_CLASS_NAME = "org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest";
    public static final String META = "meta"; //meta.yml file
    public static final String SECRETS = "secrets"; //secrets dir and secrets.yml file
    static final String VARS = "vars"; //coab-vars.yml
    static final String OPERATORS = "operators"; //TODO: remove, not needed anymore
}

