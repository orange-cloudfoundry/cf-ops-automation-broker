
# Next cloudflare

- return Asynchronous service instance creation response
- BrokerProcessor: preGetLastCreateOperation + postGetLastCreateOperation
- TerraformModuleProcessor reads TFState output variables to confirm last operation completion
   - prototype sample module with output variable and import associated tf state json  

Implement Repository
- Spring injection of Validation
- SpringData import
- SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?

Integration test

cloudflare delete support: DeleteModuleWithId in context
TF module delete support

---------------
# Needs discussions


core framework:
- fail to execute SampleBrokerApplication:  Empty reply from server

- typing of exceptions throw by processors: RuntimeException ?
- spaces vs tabs indentation reported by intellij
- context key: encapsulate + as immutable object ?
- upgrade to mockito 2 https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2 


