
# Next cloudflare

- Application & Integration test: 
   - @Service or @Bean in application.
   - explicit application maven module
   - git.properties injected as env vars
   - looks_up_paas_secrets_git_local_checkout


- configure catalog: not bindeable, not updeable

- add input validation to terraform module.name and outputs (from hashicorp hcl specs) to detect more issues up front if OSB-injected ids are HCL unfriendly   


Refine Repository impl
- terraform repository: 
   - test exception handling in streams.filter. Documentation is unclear about runtime exceptions 
   propagation in map() functions. Would they be caught by collectors or propagated ? 
- using spring data
    - Spring injection of Validation: constructor injection.
    - import spring data commons
    - SpringData import
    - SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?
        - https://github.com/spring-projects/spring-data-keyvalue/blob/master/src/main/asciidoc/key-value-repositories.adoc
        - https://github.com/hazelcast/spring-data-hazelcast/blob/master/src/main/java/org/springframework/data/hazelcast/HazelcastKeyValueAdapter.java
        - https://github.com/spring-projects/spring-data-keyvalue-examples/blob/master/retwisj/src/main/java/org/springframework/data/redis/samples/retwisj/redis/RetwisRepository.java
        - https://paulcwarren.github.io/spring-content/refs/release/fs-index.html
    - extract terraform state loading into a repository when needed to support a different backend than file (credhub, S3)
 

# Next core framework

- context key: encapsulate with methods + as immutable object ?
- update + bind/unbind request in context key
 
 
---------------
# Needs discussions


core framework:
- fail to execute SampleBrokerApplication:  Empty reply from server

- typing of exceptions throw by processors: RuntimeException ?
- spaces vs tabs indentation reported by intellij
- context key: encapsulate + as immutable object ?
- upgrade to mockito 2 https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2 


