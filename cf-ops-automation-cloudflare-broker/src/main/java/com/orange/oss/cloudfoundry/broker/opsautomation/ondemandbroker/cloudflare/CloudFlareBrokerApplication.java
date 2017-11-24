package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareConfig;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareRouteSuffixValidator;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.ImmutableCloudFlareConfig;
import org.springframework.beans.factory.annotation.Value;
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
    public TerraformModule terraformModuleTemplate() {
        return TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
    }


    @Bean
    public CloudFlareConfig cloudFlareConfig(@Value("${cloudflare.routeSuffix}") String routeSuffix,
                                             @Value("${cloudflare.maxExecutionDurationSeconds:600}") int maxExecutionDurationSeconds,
                                             TerraformModule template) {
        return ImmutableCloudFlareConfig.builder()
                .routeSuffix(routeSuffix)
                .template(template)
                .maxExecutionDurationSeconds(maxExecutionDurationSeconds)
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
            @Value("${cloudflare.pathToTfState}") String pathToTfState) {
        return new TerraformCompletionTracker(clock, cloudFlareConfig.getMaxExecutionDurationSeconds(), pathToTfState);
    }

    @Bean
    public static TerraformRepository.Factory getFactory(
            @Value("${cloudflare.pathTFSpecs}") String pathtoTerraformSpecs,
            @Value("${clouflare.filePrefix:cloudflare-instance-}") String filePrefix) {
        return path -> new FileTerraformRepository(path.resolve(pathtoTerraformSpecs), filePrefix);
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
    public BrokerProcessor gitProcessor(
            @Value("${git.user}") String gitUser,
            @Value("${git.password}") String gitPassword,
            @Value("${git.url}") String gitUrl,
            @Value("${git.committerName:@null}") String committerName,
            @Value("${git.committerEmail:@null}") String committerEmail) {
        return new GitProcessor(gitUser, gitPassword, gitUrl, committerName, committerEmail);
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
