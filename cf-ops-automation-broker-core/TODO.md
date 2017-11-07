
# Next cloudflare

- Spring injection of Validation
- SpringData import
- SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?

- Id in TerraformModule. Maps to filePath
    Concrete modules pick the Id
     
- CloudFlare Id: service instance guid ?
    using a route in path is a smell
        encoding
        injections
        too large names
        too dependent on backend key restrictions

=> Id is for now service instance id.

- use TF module in core logic and refine the contract
    - reconsider jackson ?

---------------
# Needs discussions


core framework:
- context key as immutable object


