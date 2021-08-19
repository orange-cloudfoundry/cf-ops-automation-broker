package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.PlanUpgradeValidatorProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitRepoFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.RetrierGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.SimpleGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.BoshProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxySkippedDeprovisionProxy;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineCompletionTracker;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PlanUpgradeValidatorProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.ReadOnlyServiceInstanceBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.SecretsGenerator;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.TemplatesGenerator;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.VarsFilesYmlFormatter;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({GitProperties.class, PipelineProperties.class, OsbProxyProperties.class,
    PlanUpgradeValidatorProperties.class})
public class BoshBrokerApplication {

    @SuppressWarnings("WeakerAccess")
    static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-templates.";
    static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    private static final Logger logger = LoggerFactory.getLogger(BoshBrokerApplication.class.getName());


    public static void main(String[] args) {
        SpringApplication.run(BoshBrokerApplication.class, args);
    }


    // We specify the annotation on the bean factory method in order to leverage GitProperties
    // but specify a different prefix
    @ConfigurationProperties(prefix = "git.paas-secret")
    @Bean
    public GitProperties secretsGitProperties() {
        return new GitProperties();
    }

    @ConfigurationProperties(prefix = "git.paas-template")
    @Bean
    public GitProperties templateGitProperties() {
        return new GitProperties();
    }

    @ConfigurationProperties(prefix = "deployment")
    @Bean
    public DeploymentProperties deploymentProperties() {
        return new DeploymentProperties(); //would 
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OsbProxy createOsbProxy(OsbProxyProperties osbProxyProperties, OsbClientFactory clientFactory) {
        OsbProxyImpl osbProxyImpl = new OsbProxyImpl(
            osbProxyProperties.getOsbDelegateUser(),
            osbProxyProperties.getOsbDelegatePassword(),
            osbProxyProperties.getBrokerUrlPattern(),
            clientFactory);
        if (osbProxyProperties.isSkipDeProvision()) {
            logger.info("Configured to not delegate unprovision requests to inner broker. This support undeletes.");
            return new OsbProxySkippedDeprovisionProxy(osbProxyImpl);
        } else {
            return osbProxyImpl;
        }
    }

    @Bean
    public PipelineCompletionTracker pipelineCompletionTracker(Clock clock, OsbProxyProperties osbProxyProperties,OsbProxy createServiceInstanceResponseOsbProxy, SecretsGenerator secretsGenerator) {

        return new PipelineCompletionTracker(
                clock,
                osbProxyProperties.getMaxExecutionDurationSeconds(),
                createServiceInstanceResponseOsbProxy,
                secretsGenerator
        );
    }

    @Bean
    public SecretsGenerator secretsGenerator(
        DeploymentProperties deploymentProperties,
        VarsFilesYmlFormatter varsFilesYmlFormatter) {
        return new SecretsGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getModelDeploymentShortAlias(),
                deploymentProperties.getModelDeploymentSeparator(),
            varsFilesYmlFormatter
        );
    }

    @Bean
    public VarsFilesYmlFormatter varsFilesYmlFormatter(DeploymentProperties deploymentProperties) {
        return new VarsFilesYmlFormatter(deploymentProperties.isCoabVarsInputValidationDisabled());
    }

    @Bean
    public TemplatesGenerator templatesGenerator(
        DeploymentProperties deploymentProperties,
        VarsFilesYmlFormatter varsFilesYmlFormatter) {
        return new TemplatesGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getModelDeploymentShortAlias(),
                deploymentProperties.getModelDeploymentSeparator(),
            varsFilesYmlFormatter
        );
    }

    @Bean
    public BrokerProcessor readOnlyServiceInstanceBrokerProcessor(DeploymentProperties deploymentProperties) {
        return new ReadOnlyServiceInstanceBrokerProcessor(deploymentProperties.isServiceInstanceReadOnlyMode(),
            deploymentProperties.getServiceInstanceReadOnlyMessage());
    }

    @Bean
    public BrokerProcessor planUpdateValidatorBrokerProcessor(PlanUpgradeValidatorProperties planUpgradeValidatorProperties) {
        return new PlanUpgradeValidatorProcessor(planUpgradeValidatorProperties);
    }


    @Bean
    public BrokerProcessor boshProcessor(Clock clock,
                                         TemplatesGenerator templatesGenerator,
                                         SecretsGenerator secretsGenerator,
                                         PipelineCompletionTracker pipelineCompletionTracker,
                                         DeploymentProperties deploymentProperties) {
        return new BoshProcessor(
                TEMPLATES_REPOSITORY_ALIAS_NAME,
                SECRETS_REPOSITORY_ALIAS_NAME,
                templatesGenerator,
                secretsGenerator,
                pipelineCompletionTracker,
                deploymentProperties.getBrokerDisplayName(),
                deploymentProperties.getModelDeploymentShortAlias(),
                deploymentProperties.getModelDeploymentSeparator(),
                deploymentProperties.getDashboardUrlTemplate());
    }

    @Bean
    public RetryPolicy<Object> gitSecretsRetryPolicy(GitProperties secretsGitProperties) {
        return secretsGitProperties.getRetry().toRetryPolicy();
    }

    @Bean
    public RetryPolicy<Object> gitTemplateRetryPolicy(GitProperties templateGitProperties) {
        return templateGitProperties.getRetry().toRetryPolicy();
    }

    @Bean
    public GitManager simpleSecretsGitManager(GitProperties secretsGitProperties) {
        return gitManager(secretsGitProperties, SECRETS_REPOSITORY_ALIAS_NAME);
    }

    @Bean
    public GitManager simpleTemplatesGitManager(GitProperties templateGitProperties) {
        return gitManager(templateGitProperties, TEMPLATES_REPOSITORY_ALIAS_NAME);
    }

    @Bean
    public GitManager retrierSecretsManager(GitManager simpleSecretsGitManager, RetryPolicy<Object> gitSecretsRetryPolicy) {
        return new RetrierGitManager(SECRETS_REPOSITORY_ALIAS_NAME, simpleSecretsGitManager, gitSecretsRetryPolicy);
    }

    @Bean
    public GitManager retrierTemplatesManager(GitManager simpleTemplatesGitManager, RetryPolicy<Object> gitTemplateRetryPolicy) {
        return new RetrierGitManager(TEMPLATES_REPOSITORY_ALIAS_NAME, simpleTemplatesGitManager, gitTemplateRetryPolicy);
    }

    /**
     * Constructs the default Context used as a pool key in {@link PooledGitManager}.
     * For paas-secret git repo, this key is empty
     */
    @Bean
    public Context secretsDefaultEagerPoolingContext() {
        return new Context();
    }

    /**
     * Constructs the default Context used as a pool key in {@link PooledGitManager}.
     * For paas-templates git repo, this key includes the name of the git branches to use
     */
    @Bean
    public Context templatesDefaultEagerPoolingContext(PipelineProperties pipelineProperties) {
        Context context = new Context();
        registerPaasTemplatesBranches(context, pipelineProperties);
        return context;
    }

    @Bean(initMethod = "init")
    public GitManager poolingSecretsGitManager(GitManager retrierSecretsManager,
        Context secretsDefaultEagerPoolingContext, GitProperties secretsGitProperties) {
        PooledGitRepoFactory factory = new PooledGitRepoFactory(retrierSecretsManager);
        return new PooledGitManager(factory, SECRETS_REPOSITORY_ALIAS_NAME, retrierSecretsManager,
            secretsDefaultEagerPoolingContext, secretsGitProperties.getPoolingProperties());
    }

    @Bean(initMethod = "init")
    public GitManager poolingTemplatesGitManager(GitManager retrierTemplatesManager,
        Context templatesDefaultEagerPoolingContext, GitProperties templateGitProperties) {
        PooledGitRepoFactory factory = new PooledGitRepoFactory(retrierTemplatesManager);
        return new PooledGitManager(factory, TEMPLATES_REPOSITORY_ALIAS_NAME, retrierTemplatesManager, templatesDefaultEagerPoolingContext,
			templateGitProperties.getPoolingProperties());
    }

    @Bean
    public GitManager secretsGitManager(GitProperties secretsGitProperties, GitManager retrierSecretsManager, GitManager poolingSecretsGitManager) {
        GitManager gitManager;
        if (secretsGitProperties.isUsePooling()) {
            gitManager= poolingSecretsGitManager;
        } else {
            gitManager = retrierSecretsManager;
        }
        return gitManager;
    }

    @Bean
    public GitManager templatesGitManager(GitProperties templateGitProperties, GitManager retrierTemplatesManager, GitManager poolingTemplatesGitManager) {
        GitManager gitManager;
        if (templateGitProperties.isUsePooling()) {
            gitManager= poolingTemplatesGitManager;
        } else {
            gitManager = retrierTemplatesManager;
        }
        return gitManager;
    }

    @Bean
    public BrokerProcessor templateGitProcessor(GitManager templatesGitManager) {
        return new GitProcessor(templatesGitManager, TEMPLATES_REPOSITORY_ALIAS_NAME);
    }

    @Bean
    public BrokerProcessor secretsGitProcessor(GitManager secretsGitManager) {
        return new GitProcessor(secretsGitManager, SECRETS_REPOSITORY_ALIAS_NAME);
    }

    private GitManager gitManager(GitProperties gitProperties, String repoAliasName) {
        return new SimpleGitManager(
                gitProperties.getUser(),
                gitProperties.getPassword(),
                gitProperties.getUrl(),
                gitProperties.committerName(),
                gitProperties.committerEmail(),
                repoAliasName);
    }

    /**
     * Default processors are used for all OSB operations except delete
     */
    @Bean
    public List<BrokerProcessor> defaultProcessors(BrokerProcessor boshProcessor,
        BrokerProcessor secretsGitProcessor, BrokerProcessor templateGitProcessor,
        BrokerProcessor paasTemplateContextFilter, BrokerProcessor readOnlyServiceInstanceBrokerProcessor,
        BrokerProcessor planUpdateValidatorBrokerProcessor) {
        List<BrokerProcessor> defaultProcessors = new ArrayList<>();

        // reject service instance operations at first even before clone
        defaultProcessors.add(readOnlyServiceInstanceBrokerProcessor);

        defaultProcessors.add(planUpdateValidatorBrokerProcessor);

        //selectively perform git clones depending on OSB lifecycle step
        defaultProcessors.add(paasTemplateContextFilter);
        // on create/update git push will trigger 1st for paas-templates and then 2nd for paas-secrets,
        // reducing occurences of fail-fast consistency check failures
        // see related https://github.com/orange-cloudfoundry/cf-ops-automation/issues/201
        defaultProcessors.add(secretsGitProcessor);  //commit push is on post-delete executed in reverse order
        defaultProcessors.add(templateGitProcessor);
        defaultProcessors.add(boshProcessor);
        return defaultProcessors;
    }

    /**
     * Delete processors are especially crafted in order to control the order of git processor:
     * delete secrets first (to avoid locking COA in its consistency check) and them templates.
     */
    @Bean
    public List<BrokerProcessor> deleteProcessors(BrokerProcessor boshProcessor,
        BrokerProcessor secretsGitProcessor,
        BrokerProcessor templateGitProcessor, BrokerProcessor paasTemplateContextFilter,
        BrokerProcessor readOnlyServiceInstanceBrokerProcessor) {
        List<BrokerProcessor> deleteProcessors = new ArrayList<>();

        // reject service instance operations at first even before clone
        deleteProcessors.add(readOnlyServiceInstanceBrokerProcessor);

        //selectively perform git clones depending on OSB lifecycle step
        deleteProcessors.add(paasTemplateContextFilter);
        // on OSB delete, remove files & git push 1st for paas-secrets repo
        //and then 2nd for paas-templates repo,
        // reducing occurences of fail-fast consistency check failures
        // see related https://github.com/orange-cloudfoundry/cf-ops-automation/issues/201
        deleteProcessors.add(templateGitProcessor); //commit push is on post-delete executed in reverse order
        deleteProcessors.add(secretsGitProcessor);
        deleteProcessors.add(boshProcessor);
        return deleteProcessors;
    }

    @Bean
    public ProcessorChain processorChain(
        //Not clear while qualifier is indeed required, otherwise spring does not initialize delete processors
        @Qualifier(value="defaultProcessors") List<BrokerProcessor> defaultProcessors,
        @Qualifier(value="deleteProcessors") List<BrokerProcessor> deleteProcessors) {

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(defaultProcessors, deleteProcessors,sink);
    }

    /**
     * Configures keys in the Context for use by SimpleGitProcessor to control on which OSB
     * lifecycle steps to git clone paas-templates repo
     */
    @Bean
    public BrokerProcessor paasTemplateContextFilter(PipelineProperties pipelineProperties) {
        return new DefaultBrokerProcessor() {

            // for necessary steps, configure the right branch to fetch
            @Override
            public void preCreate(Context ctx) { registerPaasTemplatesBranches(ctx, pipelineProperties); }
            @Override
            public void preUpdate(Context ctx) { registerPaasTemplatesBranches(ctx, pipelineProperties); }
            @Override
            // delete step now also cleans up paas-templates
            public void preDelete(Context ctx) { registerPaasTemplatesBranches(ctx, pipelineProperties); }

            //for steps only requiring paas-secrets, don't clone paas-templates
            @Override
            public void preGetLastOperation(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }

            //for yet unimplemented steps, don't clone paas-templates
            @Override
            public void preBind(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }
            @Override
            public void preUnBind(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }
            @Override
            public void preGetInstance(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }

        };
    }

    private void registerPaasTemplatesBranches(Context ctx, PipelineProperties pipelineProperties) {
        ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.checkOutRemoteBranch, pipelineProperties.getCheckOutRemoteBranch()); //"develop"
        ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.createBranchIfMissing, pipelineProperties.getCreateBranchIfMissing()); //"feature-coadepls-cassandra-serviceinstances"
    }
    private void skipPaasTemplateGitCloneAndPush(Context ctx) {
        ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.ignoreStep, "true"); //"develop"
    }


}
