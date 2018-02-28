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

    
- add repo alias into the tmp dir name

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
       
- Implement OSB provision delegation to nested cassandra broker
   - refine PipelineCompletionTracker to call OSB client bind/unbind
     - core framework: create/delete service binding
     - delegate delete request in PipelineCompletionTracker to OSBProxy


- refine CassandraServiceProvisionningTest to use OSB client instead of raw rest assured
   - CassandraServiceProvisionningTest rest-assured based client which is not compliant w.r.t. "X-Broker-API-Originating-Identity" mandatory header.
   - clean up associated workaround in OsbClientFeignConfig 
- clean up maven dependency management for wiremock: factor out version across modules


- OSBProxy future refinements     
     - propagate description from nested broker 
     - strip out some arbitrary params
     - better map service plans
     - modularize mapping as injected strategies
 
- Refine tarball automatic deployment to filter on the current cassandra PR branch name

- add support for returning templated dashboard in Cassandra processor provision responses    


- core framework: update service 


--------------------------------

- Refactor to generalize to another deployment (e.g. mysql)
   - Convert constants from CassandraProcessorConstants into Properties (ONGOING)
   - Change secrets generation from files to symlinks (ONGOING)
   - Refactor bosh templates (TODO)
       - static operators instead of cassandra specific operators
       - propagate OSB data as bosh vars
   - Introduce configuration class and builder in order to hold secrets/templates static information (TODO)
   
   
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

