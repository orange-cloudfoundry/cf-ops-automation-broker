
- consider recording git commit log into build
    https://github.com/ktoso/maven-git-commit-id-plugin#using-the-plugin
    https://stackoverflow.com/questions/40446275/include-git-commit-hash-in-jar-version

- fix file system leak, likely git repo leak
       2018-02-26T09:37:52.96+0100 [APP/PROC/WEB/0] OUT Caused by: org.eclipse.jgit.api.errors.TransportException: No space left on device

    vcap@2438b2fe-2c49-4e21-4862-8ed8:~/tmp$ ls -alrH | head
    total 556
    drwxr-xr-x   2 vcap vcap  4096 Feb 26 10:53 tomcat-docbase.6964236809616157912.8080
    drwxr-xr-x   3 vcap vcap  4096 Feb 26 10:53 tomcat.4424243097414663641.8080
    drwx------   2 vcap vcap  4096 Feb 26 11:16 broker-990852131337233226
    drwx------   2 vcap vcap  4096 Feb 26 11:43 broker-979417479625278769
    drwx------   2 vcap vcap  4096 Feb 26 11:39 broker-941872569701712118
    [...]
    vcap@2438b2fe-2c49-4e21-4862-8ed8:~/tmp$ ls -alrH | grep broker | wc -l
    133
      

- fix OOM  in the broker (currently configured with 1GB RAM)
    - increase bindly capacity and check if OOMs persist
    - diagnose
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

