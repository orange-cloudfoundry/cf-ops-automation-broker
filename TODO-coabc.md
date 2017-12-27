   
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
            - manually instead of trying to share a single feign client in the spring context https://cloud.spring.io/spring-cloud-netflix/single/spring-cloud-netflix.html#_creating_feign_clients_manually
                - naive implementation prevents sharing of REST API annotation with spring-cloud-service-broker
                    - solution is to also inject the springmvc contract. Hinted from https://github.com/spring-cloud/spring-cloud-netflix/issues/1834
                    - TODO: move this into a new OsbClientBuilder bean + check path for destroy/GC of feign clients
                    - TODO: use spring-boot-service-broker springMVC annotations 
            - through spring leveraging the existing prototype scope assigned to some beans in FeignClientsConfiguration 
        - copy/paste from sec-group-broker-filter (service instance, service binding) 
            - FilteredBrokerFeignConfig: basic auth: 
                - need rework to fetch from credhub context
            - OkHttpClientConfig: to support self signed certs, may not be required anymore
            - pom.xml
                -  starter-feign
                    - exclusion was designed for cf-java-client conflict 
                - check if feign-okhttp still needed
        - TBC integration tests using
            - wiremock
            - rest assured
            - springbootapp: 
               - static-cred-broke
               - inlined controller
        - move feign out of broker-core into its own module
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

