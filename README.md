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

    @SpringBootApplication
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

## Configuring the service broker catalog

The framework requires the broker to just provide an implementation of a [`Catalog` bean](https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/master/src/main/java/org/springframework/cloud/servicebroker/model/Catalog.java).

### Using @Configuration and @Bean to inject a Catalog bean

There is an example of this approach in the [sample broker](cf-ops-automation-sample-broker/src/main/java/com/orange/oss/cloudfoundry/broker/opsautomation/ondemandbroker/sample/BrokerCatalogConfig.java).

    @Configuration
    public class BrokerCatalogConfig {
    	@Bean
    	public Catalog catalog() {
    		return new Catalog(Collections.singletonList(
    				new ServiceDefinition(
    						"ondemand-service",
    						"ondemand",
    						"A simple ondemand service broker implementation",
    						true,
    						false,
    						Collections.singletonList(
    								new Plan("ondemand-plan",
    										"default",
    										"This is a default ondemand plan.  All services are created equally.",
    										getPlanMetadata())),
    						Arrays.asList("ondemand", "document"),
    						getServiceDefinitionMetadata(),
    						null,
    						null)));
    	}
    }

### Using spring-boot-starter-servicebroker-catalog to configure your service broker catalog

[spring-boot-starter-servicebroker-catalog](spring-boot-starter-servicebroker-catalog) provides an opinionated spring boot 'starter' to simplify your catalog configuration.

To benefit from the starter , add it to your POM:

        <dependency>
            <groupId>com.orange.oss.cloudfoundry.broker.opsautomation</groupId>
            <artifactId>spring-boot-starter-servicebroker-catalog</artifactId>
            <version>last_version</version>
        </dependency>
        
You can then configure the [`Catalog`](https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker/blob/master/src/main/java/org/springframework/cloud/servicebroker/model/Catalog.java)
using properties files, YAML files, environment variables or command-line arguments.
There is an example of this approach in the [sample broker](cf-ops-automation-sample-broker/src/main/resources/application.yml).

Please, notice that you can also use `CATALOG_YML` environment variable to set catalog config in a YAML format.

```shell
#export catalog.yml file content as an env variable
export CATALOG_YML="$(cat catalog.yml)"

```

See [catalog.yml](cf-ops-automation-sample-broker/catalog.yml) for details.




 




