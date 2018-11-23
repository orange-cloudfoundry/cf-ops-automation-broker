package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({GitProperties.class, PipelineProperties.class, OsbProxyProperties.class})
public class BoshBrokerApplication {

    @SuppressWarnings("WeakerAccess")
    static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-templates.";
    static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

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
        return new OsbProxyImpl(
                osbProxyProperties.getOsbDelegateUser(),
                osbProxyProperties.getOsbDelegatePassword(),
                osbProxyProperties.getBrokerUrlPattern(),
                clientFactory);
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
            DeploymentProperties deploymentProperties) {
        return new SecretsGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getModelDeploymentShortAlias()
        );
    }

    @Bean
    public TemplatesGenerator templatesGenerator(DeploymentProperties deploymentProperties) {
        return new TemplatesGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getModelDeploymentShortAlias(),
                new VarsFilesYmlFormatter() //externalize if needed
        );
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
                deploymentProperties.getModelDeploymentShortAlias());
    }

    @Bean
    public BrokerProcessor secretsGitProcessor(GitProperties secretsGitProperties) {
        return makeBrokerProcessor(secretsGitProperties, SECRETS_REPOSITORY_ALIAS_NAME);
    }

    @Bean
    public BrokerProcessor templateGitProcessor(GitProperties templateGitProperties) {
        return makeBrokerProcessor(templateGitProperties, TEMPLATES_REPOSITORY_ALIAS_NAME);
    }

    private BrokerProcessor makeBrokerProcessor(GitProperties secretsGitProperties, String repoAliasName) {
        GitManager simpleGitManager = new SimpleGitManager(
                secretsGitProperties.getUser(),
                secretsGitProperties.getPassword(),
                secretsGitProperties.getUrl(),
                secretsGitProperties.committerName(),
                secretsGitProperties.committerEmail(),
                repoAliasName);
        GitManager gitManager;
        if (!secretsGitProperties.isUsePooling()) {
            gitManager = simpleGitManager;
        } else {
            PooledGitRepoFactory factory = new PooledGitRepoFactory(simpleGitManager);
            gitManager= new PooledGitManager(factory, repoAliasName, simpleGitManager);
        }
        return new GitProcessor(gitManager, repoAliasName);
    }


    @Bean
    public ProcessorChain processorChain(BrokerProcessor boshProcessor, BrokerProcessor secretsGitProcessor, BrokerProcessor templateGitProcessor, BrokerProcessor paasTemplateContextFilter) {
        List<BrokerProcessor> processors = new ArrayList<>();

        processors.add(paasTemplateContextFilter);
        // git push wil trigger 1st for paas-templates and then 2nd for paas-secrets,
        // reducing occurences of fail-fast consistency check failures
        // see related https://github.com/orange-cloudfoundry/cf-ops-automation/issues/201
        processors.add(secretsGitProcessor);
        processors.add(templateGitProcessor);
        processors.add(boshProcessor);

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }

    @Bean
    public BrokerProcessor paasTemplateContextFilter(PipelineProperties pipelineProperties) {
        return new DefaultBrokerProcessor() {

            // for necessary steps, configure the right branch to fetch
            @Override
            public void preCreate(Context ctx) { registerPaasTemplatesBranches(ctx); }

            //for steps only requiring paas-secrets, don't clone paas-templates
            @Override
            public void preGetLastOperation(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }
            @Override
            public void preDelete(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }

            //for yet unimplemented steps, don't clone paas-templates
            @Override
            public void preBind(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }
            @Override
            public void preUnBind(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }
            @Override
            public void preUpdate(Context ctx) { skipPaasTemplateGitCloneAndPush(ctx); }

            private void registerPaasTemplatesBranches(Context ctx) {
                ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.checkOutRemoteBranch.toString(), pipelineProperties.getCheckOutRemoteBranch()); //"develop"
                ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.createBranchIfMissing.toString(), pipelineProperties.getCreateBranchIfMissing()); //"feature-coadepls-cassandra-serviceinstances"
            }
            private void skipPaasTemplateGitCloneAndPush(Context ctx) {
                ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.ignoreStep.toString(), "true"); //"develop"
            }

        };
    }


}
