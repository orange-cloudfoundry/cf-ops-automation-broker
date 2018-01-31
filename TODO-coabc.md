- Release on-demand cassandra bosh deployment
    - mvn release / github release
    - automate broker deployment in paas-template from binary in github release

- Smoke tests: create service instance, get service instance (wait for success), delete service instance

- Implement OSB provision delegation to nested cassandra broker
   - refine PipelineCompletionTracker to call OSB client create/delete/bind/unbind
      - refine PipelineCompletionTracker
         - investigate refactoring (to merge with TF support, support credhub)
            - 1 PendingAsyncProcessing: isCompleted(): boolean
               - terraform module impl:   isCompleted(Path gitWorkDir,     String moduleName=serviceInstanceId) (respectively for provision and unprovision)
               - pipeline processor impl: isCompleted(Path secretsWorkDir, String serviceInstanceId) 
            - 0-N: SyncProcessing: apply(ServiceBrokerReq req, ServiceBrokerResponse resp): ServiceBrokerResponse 
               - OSB client sync delegate impl
                  - construct OSB client: construct url from serviceInstanceId, and configured static pwd
                  - fetch catalog
                  - map req
                  - provisionning instance
                  - map resp
            - 0-N: ASyncProcessing: apply(ServiceBrokerReq req, ServiceBrokerResponse resp): void
               - OSB client async delegate impl
               - TF module binding impl 
                  
           Challenges and differences:
              - TerraformCompletionTracker uses async delete
              - TerraformCompletionTracker does not yet support binding
           => too early to refactor, keep duplication for now, try to keep impls close
           
      - add component to map OSB request (serviceid, planid, in future strip out some arbitrary params)
         - takes a Catalog bean from which it fetches serviceid and planid

- Implement OSB binding delegation to nested cassandra broker
   - core framework: create/delete service binding 

- Refine timeout implementation: support configuring timeout in the broker (currently hardcoded)
    - refactor test to properly assert timeout first
        - remove Json litteral and manual date editing ?  
        - refactor Time support to manipulate duration instead of dates



--------------------------------

- Refactor to generalize to another deployment (e.g. mysql)
   - Bosh templates refactoring
       - static operators instead of cassandra specific operators
       - propagate OSB data as bosh vars
   - convert constants from CassandraProcessorConstants into Properties
   


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
    - generates new bosh deployment: writes paas-template/paas-secrets (*Generator classes)
        - improve tests readability (JUnit rules)
        - improve tests coverage (file content)
        - improve consistency between secrets and template (files vs symbolic links)
        - bean spring ?

    - removes bosh deployment: deletes paas-secrets (*Generator classes)
   
    - tracks completion of bosh deployment: watches rendered manifest file (PipelineCompletionTracker class) 

    - IT with paas-template/paas-secret: Inspired from GitIT
    - (later once broker password are specific to each instance): specify credhub keys to fetch broker password 
       - workaround: broker password passed to coab as env in its manifest.yml

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

- ~~osb processor:~~
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
        - fix depreciation 
            Warning:(93, 17) java: sslSocketFactory(javax.net.ssl.SSLSocketFactory) in okhttp3.OkHttpClient.Builder has been deprecated
               Actually check whether okHttpClient support for self signed certs and http proxy is still usefull, otherwise remove it
        - fix OkHttpClientConfig warnings    
     
- credhub write support


- ~~osb processor: create/delete service binding~~

- ~~credhub processor~~
    - integration test

