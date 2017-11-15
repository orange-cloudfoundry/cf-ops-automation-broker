
# Next cloudflare

- Async create completion

   - store timestamp into context and handle timeout for pending exec.
       - add timeout threashold into config
       - refine CompletionTrakcer.getModuleExecStatus to evaluate timeout


- cloudflare delete support


- Integration test: 
   - @Service or @Bean in application.
   - explicit application maven module
   - git.properties injected as env vars


- catalog: not bindeable
                                                            
Implement Repository
- Spring injection of Validation: constructor injection.
- SpringData import
- SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?
    - https://github.com/spring-projects/spring-data-keyvalue/blob/master/src/main/asciidoc/key-value-repositories.adoc
    - https://github.com/hazelcast/spring-data-hazelcast/blob/master/src/main/java/org/springframework/data/hazelcast/HazelcastKeyValueAdapter.java
    - https://github.com/spring-projects/spring-data-keyvalue-examples/blob/master/retwisj/src/main/java/org/springframework/data/redis/samples/retwisj/redis/RetwisRepository.java
    - https://paulcwarren.github.io/spring-content/refs/release/fs-index.html
    - 



 

# Next core framework

- delete request in context key 
- inline ProcessorChain.create() + pass in context to other
- context key: encapsulate with methods + as immutable object ?
 
 
---------------
# Needs discussions


core framework:
- fail to execute SampleBrokerApplication:  Empty reply from server

- typing of exceptions throw by processors: RuntimeException ?
- spaces vs tabs indentation reported by intellij
- context key: encapsulate + as immutable object ?
- upgrade to mockito 2 https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2 


