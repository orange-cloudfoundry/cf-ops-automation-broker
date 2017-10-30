# cf-ops-automation-broker [![CI](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker)
Open Service Broker API for Orange cf-ops-automation pipelines generation

Status:
Latest news:
- we have developed and used a concourse base contiuous delivery CD mechanism. https://github.com/orange-cloudfoundry/cf-ops-automation
- this mechanism, coupled with best pratices paas templates can deploy and maintain bosh masnifests, and terraform spec files
- secrets generation, retrieval, and management can be handled with credhub

Evaluating a new scenario:
- develop a service broker which generates cf-ops-automation compatible manifests files.
- the service broker relies on concourse pipelines to apply manifests file, deploy and update concourse and terraform resources

## Getting Started

A sample [service broker](cf-ops-automation-sample-broker) project is available.

Add dependencies to your project's build file. 

Maven example: 

    <dependencies>
        ...
          <dependency>
                    <groupId>com.orange.oss.cloudfoundry.broker.opsautomation</groupId>
                    <artifactId>cf-ops-automation-broker-core</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
          </dependency>
        ...
    </dependencies>
    
## Configuring the broker

The framework requires the broker to just provide an implementation of a [`ProcessorChain` bean](cf-ops-automation-broker-framework/src/main/java/com/orange/oss/cloudfoundry/broker/opsautomation/ondemandbroker/processors/ProcessorChain.java).

    @ComponentScan
    @EnableAutoConfiguration
    public class Application {
    
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
         
        @Bean
        public ProcessorChain processorChain() {
            List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
            processors.add(new DefaultBrokerProcessor());
            DefaultBrokerSink sink=new DefaultBrokerSink();
            ProcessorChain chain=new ProcessorChain(processors, sink);
            return chain;
        }
    }
 
For catalog management, the framework provides a default implementation that requires the broker to just provide an implementation of a [`Catalog` bean](https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/master/src/main/java/org/springframework/cloud/servicebroker/model/Catalog.java). There is an example of this approach in the [sample broker](cf-ops-automation-sample-broker/src/main/java/com/orange/oss/cloudfoundry/broker/opsautomation/ondemandbroker/sample/BrokerCatalogConfig.java).
