- commit and restore green build on circle
- reproduce binding response issue in OsbClientTestApplicationTest 
- try to solve it in current 1.0.2 spring service broker version
- as a fallback, resume bump to springcloud2 and service broker 2   

- refine exception filtering to allow broker framework exceptions 
    from org.springframework.cloud.servicebroker.exception package to get through

- Implement OSB provision delegation to nested cassandra broker
   - refine PipelineCompletionTracker to call OSB client bind/unbind
     - DONE: ~~core framework: create/delete service binding~~
     - DONE: implement bind delegation & mapping in OSBProxy
     - DONE: implement unbind delegation & mapping in OSBProxy
        test delegateUnbind()
     - DONE: cassandra processor: delegate bind/unbind to PipelineCompletionTracker 
     - DONE: delegate delete request in PipelineCompletionTracker to OSBProxy
   - DONE: record current cassandra broker bind errors 
   - update CassandraServiceProvisionningTest + associated wiremock recordings
        - refactor CassandraServiceProvisionningTest to use OSB client instead of raw rest assured:CassandraServiceProvisionningTest rest-assured based client which is not compliant w.r.t. "X-Broker-API-Originating-Identity" mandatory header.
               
              - Pb with service provisionning: GSON mapping not resulting into CreateServiceInstanceResponse but rather Map<String,String>
                 - same in OsbClientTestApplicationTest where GSON maps to empty Map<String,String>
                    - try to turn on Feign logs. No much luck. Similar symptoms than https://github.com/spring-cloud/spring-cloud-netflix/issues/1769
                    - try step into 
                       - Spring annotation contract
                       - Json unmarshalling: GSon or Jackson ?
                       
                       - **root cause: incorrect annotation on the Feign interface**
                          - Q: how to deal with CreateServiceInstanceBindingResponse  subtypes:  CreateServiceInstanceAppBindingResponse or CreateServiceInstanceRouteBindingResponse ?
                             - A: need to annotate POJOs for deserialization https://stackoverflow.com/questions/32766922/jackson-deserialization-on-multiple-types?noredirect=1&lq=1
                                - would require modifying spring-cloud-service-broker pojo 
                                   - requires bumping version
                                      - requires bumping to springcloud 2
                          => submit an issue for now and delay this until we have to deal with route services.
                             
                       - weird provisionning response recorded: missing operation, and extra async field. Is this a side effect of the transient field hack ? 
                            "body" : "{\"async\":false,\"dashboard_url\":null}",
  
                                                                             
                    - try to bump openfeign to latest https://github.com/OpenFeign/feign/releases 9.6.0 (currently 9.5.0) https://github.com/OpenFeign/feign/blob/master/CHANGELOG.md
                       - bump spring-cloud-starter-openfeign from 1.4.0-RELEASE to 1.4.3-RELEASE ?
                          - No better https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-openfeign/1.4.3.RELEASE
                       - bump to spring-cloud-starter-openfeign 2.0.0 M6 ? Too early ? https://cloud.spring.io/spring-cloud-openfeign/
                          - No better https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-openfeign/2.0.0.M6 still pull feign 9.5.1
                       - dependency management for open-feign ?       
                       - bump spring boot to 2.0.0-RELEASE ?
                                   
                       
                 - Q: how come work on OSBProxyImpl did not yet ran into same issue ?
                 - A: OsbProxyImpl/CompletionTracker currently only leverage response status but not yet payload:
                    - sync provision/unprovision where dashboard url isn't yet dereferenced
          => this Feign/Gson issue is likely to also occur on service binding response we are dependent for
          
            
   - refine smoke tests to test C* credentials using a CF app 
   

   - future bind mapping improvements
       - factor out plan mapping into a method and then a bean 
       - support route binding responses                 



- harden smoke tests:
   - to clean up service instances 
        - failed
            - create failed
                cf s | grep 'create failed' | cut -d ' ' -f 1 | xargs -n 1  cf ds -f 
    
        - beyond the quota
   - to ease CF/concourse correlation: rename service instance with the the service instance guid
   - refactor to use more readeable syntax: trap, seq, functions, see 
      - https://github.com/cloudfoundry/capi-release/blob/develop/src/capi_utils/monit_utils.sh
      - https://github.com/cloudfoundry/cf-deployment-concourse-tasks/blob/master/bosh-delete-deployment/task


- improve coab concourse integration
    - schedule smoke test service instance automatic approval 
        login -t micro.preprod -c url  -u atc -p password ;  /usr/sbin/fly -t micro.preprod trigger-job --job=coab-depls-generated/approve-and-delete-disabled-deployments) 
    - trigger coab deployment by watching circle ci tar balls    
        
    - when osb delegation previously failed, support flag to ignore delete errors for brokers that don't maintain external state

      
    - when broker is missing
        - skip OsbProxy when manifest is missing
        - ignore OsbProxy errors during delete 
            2018-03-01T15:33:52.04+0100 [APP/PROC/WEB/0] OUT feign.FeignException: status 404 reading CatalogServiceClient#getCatalog(); content:


- automate inspection of smoke tests broker exceptions
   - in log search using specific searches
   
- stronger configuration validation to avoid following symptom: fail fast 
    - refine spring validation study, see reverted commit b031956e3091da4a77357b0216a7b718bccb6f78
      - keep @ConfigurationProperties annotation except for GitProperties to diagnose
         - stackoverflow ?
         - debugger
      - find a solution for classes that are not @Component
         - get inspiration from springboot source code itself
         - look into spring-boot-starter-validation source code 

   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT java.lang.NullPointerException: null
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at java.text.MessageFormat.applyPattern(MessageFormat.java:436) ~[na:1.8.0_131]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at java.text.MessageFormat.<init>(MessageFormat.java:362) ~[na:1.8.0_131]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at java.text.MessageFormat.format(MessageFormat.java:840) ~[na:1.8.0_131]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbProxyImpl.getBrokerUrl(OsbProxyImpl.java:246) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineCompletionTracker.buildResponse(PipelineCompletionTracker.java:86) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineCompletionTracker.getDeploymentExecStatus(PipelineCompletionTracker.java:59) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.CassandraProcessor.preGetLastOperation(CassandraProcessor.java:71) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain.getLastOperation(ProcessorChain.java:36) ~[cf-ops-automation-broker-framework-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.getLastOperation(ProcessorChainServiceInstanceService.java:109) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]

    
- GitProcessor UX improvement: add repo alias into the tmp dir name

- diagnose smoke test revealed symptom 1:

Deleting service cassandra-instance14938 in org system_domain / space coa-cassandra-broker as admin...
	
FAILED
	
Server error, status code: 409, error code: 60016, message: An operation for service instance cassandra-instance14938 is in progress.

- diagnose smoke test revealed symptom 2:

/tmp/build/140b8ac5/result-dir/lib/root_deployment.rb:36:in `block in overview_from_hash': Inconsistency detected: found enable-deployment.yml, but no deployment-dependencies.yml found at ../templates/coab-depls/cassandra_59ee1bfc-228e-403d-bbc2-6c2f8edeb80f/deployment-dependencies.yml (RuntimeError)
	
	from /tmp/build/140b8ac5/result-dir/lib/root_deployment.rb:28:in `each'
	
	from /tmp/build/140b8ac5/result-dir/lib/root_deployment.rb:28:in `overview_from_hash'
	
	from ./scripts/generate-depls.rb:98:in `<main>'

      

- instrument to diagnose future OOM  in the broker (currently configured with 1GB RAM)
        - connect newrelic
        - turn on java buildpack diagnostics
            - use recent online version
    

                2018-02-23T15:12:14.00+0100   app.crash                  coa-cassandra-broker   index: 0, reason: CRASHED, exit_description: 2 error(s) occurred:
                
                                                                                                * 2 error(s) occurred:
                
                                                                                                * Exited with status 137 (out of memory)
                                                                                                * cancelled
                                                                                                * cancelled
       

   - clean up associated workaround in OsbClientFeignConfig 
- clean up maven dependency management for wiremock: factor out version across modules


- OSBProxy/CompletionTracker future refinements     
     - propagate description from nested broker instead of hardcoded 
     - strip out some arbitrary params
     - better map service plans
     - modularize mapping as injected strategies
 
- Refine tarball automatic deployment to filter on the current cassandra PR branch name

- add support for returning templated dashboard in Cassandra processor provision responses    


- core framework: update service 


--------------------------------

- Refactor to generalize to another deployment (e.g. mysql)
   - Introduce configuration class and builder in order to hold secrets/templates static information (TODO)
   - Refactor secrets and templates generator to have little methods
   - Split Junit tests between check and generate in order to improve readability (target little methods instead of global method)
   - Use MessageFormat in StructureGeneratorHelper
   - Use Builder in Junit tests to improve readability
   - Manage “-tpl” files
   - Improve test coverage StructureGeneratorHelper
   - Update integration test to use a builder (e.g. for OSB req/resps) 
   
   
- refine logback config to 
     - include vcap request id in each trace (MDC), as thread name isn't useful
     - include higher time resolution, as to workaround out of order loggregator traces displayed (sort by logback timestamps instead of loggregator timestamps)
        - report issue with Paas ops team



- git processor
    - implement caching:
        pull --rebase instead of clone
        
    - refine test to assert git repo authentication

    - integration test: paas-template create feature-COAB-cassandra-IT:
        - assert pushed repo content ?

    - add support for replicated submodules https://github.com/orange-cloudfoundry/cf-ops-automation/issues/69 maintain a ~/.giconfig of form
             - [url "https://gitlab.internal.paas"]
                   insteadOf = "https://github.com"  
    - refactor GitProcessorContext into PreGitProcessorContext and PostGitProcessorContext
        - and prefix keys with in and out
    - Implement failIfRemoteBranchExists key
    - Implement deleteRemoteBranch key
   
    - reproduce condition observed of a commit without change list (triggered by CC clean up orphan service instances every 30s)  
           2018-02-05T11:25:46.78+0100 [APP/PROC/WEB/0] OUT 2018-02-05 10:25:46.785  INFO 12 --- [nio-8080-exec-3] c.o.o.c.b.o.o.git.GitProcessor           : [paas-template.] staged commit:  deleted:[] added:[] changed:[]
           
            commit f84fa7b0184124a706a41698950f28a2597a0237
            Author: coa-cassandra-broker
            Date:   Sun Feb 4 07:27:03 2018 +0000
            
                commit by ondemand broker
            
            commit b22dddf0f1d33df203c29700cc9948ba46e1939a
            Author: coa-cassandra-broker
            Date:   Sun Feb 4 06:57:27 2018 +0000
            
                Cassandra broker: delete instance id=be8bf409-d64b-4c26-a42b-90a65459d051
    

   
    - refactor git tests to initialize clones in the gitserver setup rather than using the gitprocessor in the test itself

    - osb client
        - fix depreciation 
            Warning:(93, 17) java: sslSocketFactory(javax.net.ssl.SSLSocketFactory) in okhttp3.OkHttpClient.Builder has been deprecated
               Actually check whether okHttpClient support for self signed certs and http proxy is still usefull, otherwise remove it
        - fix OkHttpClientConfig warnings    
     
- credhub write support


- ~~osb processor: create/delete service binding~~

- ~~credhub processor~~
    - integration test

