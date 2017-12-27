
- brainstorm alternative design than Processor chain.
   - Identified weaknesses:
      - weak untyped timing dependencies: 
         - fragile sequencing of operations
         - inflexible sequencing of operations
      - weak untyped shared datastructures: lack of encapsulation
   - make explicit design requirements
      - based on functional requirements:
         - new services
      - Q: more async use-cases ?
         - COAB based service instance/binding: terraform-based service instance/binding 
   - flesh out alternative designs:
      - pure event-based design
      - simple OO design with FSM (finite state machine/ state pattern)
   - detail migration step to new design
        
   
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
    - waits for a key in context to start in any processorchain method 
        create-service-instance: a spring-cloud-cloudfoundry-service-broker object 
        update-service-instance 
        delete-service-instance 
        create-service-binding 
        delete-service-binding 
        maintains current state in context (for credhub processor to persist it) 
        polls service instance creation 
        pushes response in the context

    - osb client
        - move feign out of broker-core into its own module
           - understand why @Import(FeignClientsConfiguration.class) can't seem to take effect when specified in OsbClientFeignConfig (and only observed to work when specified in the springbootapp) + fix related failed tests https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker/326

           - more generally take appart broker-core into smaller units
        - fix depreciation 
            Warning:(93, 17) java: sslSocketFactory(javax.net.ssl.SSLSocketFactory) in okhttp3.OkHttpClient.Builder has been deprecated
               Actually check whether okHttpClient support for self signed certs and http proxy is still usefull, otherwise remove it
        - fix OkHttpClientConfig warnings    
     
- credhub write support


- core framework: create/delete service binding 

- osb processor: create/delete service binding

- credhub processor
    - integration test

