   
- cassandra processor: 
    - triggers new bosh deployment: writes paas-template/paas-secret
        - checks prerequisites (root deployment, model deployment, ...)
        - generates paas-secrets structure (directory and files)
        - generates paas-template structure (directory, files and symbolic links) and adapts content
        - improves tests readability (JUnit rules)
        - improves tests coverage (file content)
    
    - tracks completion of bosh deployment: watches rendered manifest file 
    - IT with paas-template/paas-secret: Inspired from GitIT
    - (later once broker password are specific to each instance): specify credhub keys to fetch broker password 
       - woarkaround: broker password passed to coab as env in its manifest.yml

- cassandra service broker:
    - inspired from CloudFlareBrokerApplication
    - CassandraServiceProvisionningTest inspired from CloudFlareServiceProvisionningTest. Potentially simulating concourse observable side effects in git, by driving the embedded GitServer

- git processor
    - implement caching:
        pull --rebase instead of clone

    - integration test: paas-template create feature-COAB-cassandra-IT:
        - assert pushed repo content ?

    - add support for replicated submodules https://github.com/orange-cloudfoundry/cf-ops-automation/issues/69 maintain a ~/.giconfig of form
             - [url "https://gitlab.internal.paas"]
                   insteadOf = "https://github.com"  
    - refactor GitProcessorContext into PreGitProcessorContext and PostGitProcessorContext
        - and prefix keys with in and out
    - Implement failIfRemoteBranchExists key
    - Implement deleteRemoteBranch key
   
   
    - refactor git tests to initialize clones in the gitserver setup rather than using the gitprocessor in the test itself

- osb processor: 
    - osb client
        - dynamically instanciate FeignClient (from broker url/login/password matching service instance) 
            - use spring-boot-service-broker springMVC annotations
               - currently only exposed as non abstract classes (i.e. not interfaces)
               - currently including multiple mappings: not supported by spring-cloud-netflix (feign support): java.lang.IllegalStateException: Method createServiceInstance can only contain at most 1 value field. Found: [/{cfInstanceId}/v2/service_instances/{instanceId}, /v2/service_instances/{instanceId}]
        - implement update support in ProcessorChainServiceInstanceService
        - move feign out of broker-core into its own module
           - move OsbClientTestApplicationTest & associated TestApplication there 
           - more generally take appart broker-core into smaller units
    - waits for a key in context to start in any processorchain method 
        create-service-instance: a spring-cloud-cloudfoundry-service-broker object 
        update-service-instance 
        delete-service-instance 
        create-service-binding 
        delete-service-binding 
        maintains current state in context (for credhub processor to persist it) 
        polls service instance creation 
        pushes response in the context

     
- credhub write support


- core framework: create/delete service binding 

- osb processor: create/delete service binding

- credhub processor
    - integration test

