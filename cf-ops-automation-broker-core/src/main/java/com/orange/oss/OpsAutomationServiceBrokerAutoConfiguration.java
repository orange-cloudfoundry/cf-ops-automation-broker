package com.orange.oss;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.orange.oss.ondemandbroker",
        "com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient"})
public class OpsAutomationServiceBrokerAutoConfiguration {

}
