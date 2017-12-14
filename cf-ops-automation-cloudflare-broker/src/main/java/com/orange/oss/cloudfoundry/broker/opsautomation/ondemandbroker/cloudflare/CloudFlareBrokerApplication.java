package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({GitProperties.class, CloudFlareProperties.class})
public class CloudFlareBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudFlareBrokerApplication.class, args);
    }

    @Bean
    public TerraformModule terraformModuleTemplate() {
        return TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
    }


    @Bean
    public CloudFlareConfig cloudFlareConfig(CloudFlareProperties cloudFlareProperties,
                                             TerraformModule template) {
        return ImmutableCloudFlareConfig.builder()
                .routeSuffix(cloudFlareProperties.getRouteSuffix())
                .maxExecutionDurationSeconds(cloudFlareProperties.getMaxExecutionDurationSeconds())
                .template(template)
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public TerraformCompletionTracker terraformCompletionTracker(
            CloudFlareConfig cloudFlareConfig,
            Clock clock,
            CloudFlareProperties cloudFlareProperties) {
        return new TerraformCompletionTracker(clock, cloudFlareConfig.getMaxExecutionDurationSeconds(), cloudFlareProperties.getPathToTfState());
    }

    @Bean
    public static TerraformRepository.Factory getFactory(CloudFlareProperties cloudFlareProperties) {
        return path -> new FileTerraformRepository(path.resolve(cloudFlareProperties.getPathTFSpecs()), cloudFlareProperties.getFilePrefix());
    }


    @Bean
    public CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator(CloudFlareConfig cloudFlareConfig) {
        return new CloudFlareRouteSuffixValidator(cloudFlareConfig.getRouteSuffix());
    }

    @Bean
    public CloudFlareProcessor cloudFlareProcessor(CloudFlareConfig cloudFlareConfig, TerraformRepository.Factory repositoryFactory, TerraformCompletionTracker tracker, CloudFlareRouteSuffixValidator cloudFlareRouteSuffixValidator) {
        return new CloudFlareProcessor(cloudFlareConfig, cloudFlareRouteSuffixValidator, repositoryFactory, tracker);
    }

    @Bean
    public BrokerProcessor gitProcessor(GitProperties gitProperties) {
        return new GitProcessor(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), gitProperties.committerName(), gitProperties.committerEmail(), null, "master");
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
