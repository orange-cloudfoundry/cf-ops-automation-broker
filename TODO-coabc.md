       
- Implement OSB provision delegation to nested cassandra broker
   - refine PipelineCompletionTracker to call OSB client bind/unbind
     - core framework: create/delete service binding
     - delegate delete request in PipelineCompletionTracker to OSBProxy

- fix excessive prereqs checking for delete service instance: 
    - when delete request targets a failed service instance, manifest is missing 

- fix timeout handling in delete
    Showing info of service cassandra-instance32733 in org system_domain / space coa-cassandra-broker as admin...
    
    name:            cassandra-instance32733
    service:         cassandra-ondemand
    bound apps:      
    tags:            
    plan:            default
    description:     On demand cassandra dedicated clusters
    documentation:   
    dashboard:       
    
    Showing status of last operation from service cassandra-instance32733...
    
    status:    delete failed
    message:   Execution timeout after 2002s max is 2000
    started:   2018-02-23T14:04:47Z
    updated:   2018-02-23T14:38:20Z
    


- refine CassandraServiceProvisionningTest to use OSB client instead of raw rest assured
   - CassandraServiceProvisionningTest rest-assured based client which is not compliant w.r.t. "X-Broker-API-Originating-Identity" mandatory header.
   - clean up associated workaround in OsbClientFeignConfig 
- clean up maven dependency management for wiremock: factor out version across modules


- OSBProxy future refinements     
     - strip out some arbitrary params
     - better map service plans
     - modularize mapping as injected strategies
 
- Refine tarball automatic deployment to filter on the current cassandra PR branch name

- add support for returning templated dashboard in Cassandra processor provision responses    


- core framework: update service 


--------------------------------

- Refactor to generalize to another deployment (e.g. mysql)
   - Bosh templates refactoring
       - static operators instead of cassandra specific operators
       - propagate OSB data as bosh vars
   - convert constants from CassandraProcessorConstants into Properties
   

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

