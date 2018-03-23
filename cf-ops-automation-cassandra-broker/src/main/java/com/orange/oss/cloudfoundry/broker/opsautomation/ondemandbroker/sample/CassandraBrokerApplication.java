package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
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
public class CassandraBrokerApplication {

    @SuppressWarnings("WeakerAccess")
    static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-template.";
    static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    public static void main(String[] args) {
        SpringApplication.run(CassandraBrokerApplication.class, args);
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
        return new DeploymentProperties();
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
    public PipelineCompletionTracker pipelineCompletionTracker(Clock clock, OsbProxyProperties osbProxyProperties,OsbProxy createServiceInstanceResponseOsbProxy) {
        return new PipelineCompletionTracker(
                clock,
                osbProxyProperties.getMaxExecutionDurationSeconds(),
                createServiceInstanceResponseOsbProxy
        );
    }

    @Bean
    public SecretsGenerator secretsGenerator(
            DeploymentProperties deploymentProperties) {
        return new SecretsGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getSecrets(),
                deploymentProperties.getMeta()
        );
    }

    @Bean
    public TemplatesGenerator templatesGenerator(
            DeploymentProperties deploymentProperties
    ) {
        return new TemplatesGenerator(
                deploymentProperties.getRootDeployment(),
                deploymentProperties.getModelDeployment(),
                deploymentProperties.getTemplate(),
                deploymentProperties.getVars(),
                deploymentProperties.getOperators()
        );
    }

    @Bean
    public BrokerProcessor cassandraProcessor(Clock clock,
                                              TemplatesGenerator templatesGenerator,
                                              SecretsGenerator secretsGenerator,
                                              PipelineCompletionTracker pipelineCompletionTracker) {
        return new CassandraProcessor(
                TEMPLATES_REPOSITORY_ALIAS_NAME,
                SECRETS_REPOSITORY_ALIAS_NAME,
                templatesGenerator,
                secretsGenerator,
                pipelineCompletionTracker);
    }

    @Bean
    public BrokerProcessor secretsGitProcessor(GitProperties secretsGitProperties) {
        return new GitProcessor(
                secretsGitProperties.getUser(),
                secretsGitProperties.getPassword(),
                secretsGitProperties.getUrl(),
                secretsGitProperties.committerName(),
                secretsGitProperties.committerEmail(),
                SECRETS_REPOSITORY_ALIAS_NAME);
    }

    @Bean
    public BrokerProcessor templateGitProcessor(GitProperties templateGitProperties) {
        return new GitProcessor(
                templateGitProperties.getUser(),
                templateGitProperties.getPassword(),
                templateGitProperties.getUrl(),
                templateGitProperties.committerName(),
                templateGitProperties.committerEmail(),
                TEMPLATES_REPOSITORY_ALIAS_NAME);
    }


    @Bean
    public ProcessorChain processorChain(BrokerProcessor cassandraProcessor, BrokerProcessor secretsGitProcessor, BrokerProcessor templateGitProcessor, BrokerProcessor paasTemplateBranchSelector) {
        List<BrokerProcessor> processors = new ArrayList<>();

        processors.add(paasTemplateBranchSelector);
        processors.add(secretsGitProcessor);
        processors.add(templateGitProcessor);
        processors.add(cassandraProcessor);

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }

    @Bean
    public BrokerProcessor paasTemplateBranchSelector(PipelineProperties pipelineProperties) {
        return new DefaultBrokerProcessor() {
            @Override
            public void preCreate(Context ctx) {
                ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.checkOutRemoteBranch.toString(), pipelineProperties.getCheckOutRemoteBranch()); //"develop"
                ctx.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.createBranchIfMissing.toString(), pipelineProperties.getCreateBranchIfMissing()); //"feature-coadepls-cassandra-serviceinstances"
            }

        };
    }


}
