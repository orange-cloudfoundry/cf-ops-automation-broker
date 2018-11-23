package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.SimpleGitManager;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({GitProperties.class, TerraformProperties.class})
public class TerraformBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TerraformBrokerApplication.class, args);
    }

    @Bean
    public TerraformModule terraformModuleTemplate() {
        /*FIXME: cloudflare specifics to be moved out */
        return TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
    }


    @Bean
    public TerraformConfig cloudFlareConfig(TerraformProperties terraformProperties,
                                            TerraformModule template) {
        return ImmutableTerraformConfig.builder()
                .routeSuffix(terraformProperties.getRouteSuffix())
                .maxExecutionDurationSeconds(terraformProperties.getMaxExecutionDurationSeconds())
                .template(template)
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public TerraformCompletionTracker terraformCompletionTracker(
            TerraformConfig terraformConfig,
            Clock clock,
            TerraformProperties terraformProperties) {
        return new TerraformCompletionTracker(clock, terraformConfig.getMaxExecutionDurationSeconds(), terraformProperties.getPathToTfState());
    }

    @Bean
    public static TerraformRepository.Factory getFactory(TerraformProperties terraformProperties) {
        return path -> new FileTerraformRepository(path.resolve(terraformProperties.getPathTFSpecs()), terraformProperties.getFilePrefix());
    }


    @Bean
    public CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator(TerraformConfig terraformConfig) {
        return new CloudFlareRouteSuffixValidator(terraformConfig.getRouteSuffix());
    }

    @Bean
    public TerraformProcessor cloudFlareProcessor(TerraformConfig terraformConfig, TerraformRepository.Factory repositoryFactory, TerraformCompletionTracker tracker, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator) {
        return new TerraformProcessor(terraformConfig, cloudFlareRouteSuffixValidator, repositoryFactory, tracker);
    }

    @Bean
    public BrokerProcessor gitProcessor(GitProperties gitProperties) {
        SimpleGitManager gitManager = new SimpleGitManager(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), gitProperties.committerName(), gitProperties.committerEmail(), null);
        return new GitProcessor(gitManager, null);
    }

    @Bean
    public ProcessorChain processorChain(BrokerProcessor cloudFlareProcessor, BrokerProcessor gitProcessor) {
        List<BrokerProcessor> processors = new ArrayList<>();


        processors.add(gitProcessor); //needs to be 1st
        processors.add(cloudFlareProcessor);

        //Add git processor: See GitTest

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }


}
