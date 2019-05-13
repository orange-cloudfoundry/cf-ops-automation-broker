package com.orange.oss;

import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = { ProcessorChainServiceInstanceService.class})
public class OpsAutomationServiceBrokerAutoConfiguration {

}
