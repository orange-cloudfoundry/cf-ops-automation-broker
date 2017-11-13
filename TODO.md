
# Next cloudflare

- Async create completion
             
             
         
   - output variable and import associated tf state json:
     - model output along with module invocation 
     - assign root outputs according to template in CloudFlareBrokerProcessor

   - return Asynchronous service instance creation response
   - CloudFlareBrokerProcessor: preGetLastCreateOperation + postGetLastCreateOperation
     - TerraformModuleProcessor reads TFState output variables to confirm last operation completion


- Consider bean composition instead of module chain
   - unique route validation
   - cloudflare delete support: DeleteModuleWithId in context
      TF module delete support


- Integration test: 
   - @Service or @Bean in application.
   - explicit application maven module
   - git.properties injected as env vars


- catalog: not bindeable
                                                            
Implement Repository
- Spring injection of Validation: constructor injection.
- SpringData import
- SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?



 

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


