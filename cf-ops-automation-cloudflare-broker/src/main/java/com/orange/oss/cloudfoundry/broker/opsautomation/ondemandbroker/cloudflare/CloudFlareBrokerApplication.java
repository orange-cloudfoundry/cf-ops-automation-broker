package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.FileTerraformRepository;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformCompletionTracker;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformModule;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.TerraformRepository;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class CloudFlareBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudFlareBrokerApplication.class, args);
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
    public ProcessorChain processorChain(BrokerProcessor cloudFlareProcessor, BrokerProcessor secretsGitProcessor, BrokerProcessor templateGitProcessor) {
        List<BrokerProcessor> processors = new ArrayList<>();


        processors.add(secretsGitProcessor);
        processors.add(templateGitProcessor);
        processors.add(cloudFlareProcessor);

        //Add git processor: See GitTest

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }


}
