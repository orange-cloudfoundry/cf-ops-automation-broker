
# Next cloudflare

- add input validation to terraform module.name and outputs (from hashicorp hcl specs)
- test presence of extra tf files in the repository directory
- then refactor repository impl: extract duplicate code

- exception handling in streams
- optimize repository / caching ??
- rename terraformRepository into TerraformModuleRepository ?
- import spring data commons

Implement Repository
- looks_up_paas_secrets_git_local_checkout
- Spring injection of Validation: constructor injection.
- SpringData import
- SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?
    - https://github.com/spring-projects/spring-data-keyvalue/blob/master/src/main/asciidoc/key-value-repositories.adoc
    - https://github.com/hazelcast/spring-data-hazelcast/blob/master/src/main/java/org/springframework/data/hazelcast/HazelcastKeyValueAdapter.java
    - https://github.com/spring-projects/spring-data-keyvalue-examples/blob/master/retwisj/src/main/java/org/springframework/data/redis/samples/retwisj/redis/RetwisRepository.java
    - https://paulcwarren.github.io/spring-content/refs/release/fs-index.html
    - 


- Integration test: 
   - @Service or @Bean in application.
   - explicit application maven module
   - git.properties injected as env vars

- configure catalog: not bindeable, not updeable


 

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


