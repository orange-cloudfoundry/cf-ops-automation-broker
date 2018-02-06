- fix OSB request JSON serialization in operation state: query params fields are marked as @JsonIgnore and thus missing from serialization (service instance id, service definition id, service binding id)

- fix git processor issues reported using paas-template
    - harden cleanup of git clones
        - add diagnostic traces when clean fails
        - invoke clean up even if add/commit/push fail (currently skipped) 
    - clone paas-template as a private gitlab repo to reproduce issue more easily without impacting the team
    - refine logback config to 
         - include vcap request id in each trace (MDC), as thread name isn't useful
         - include higher time resolution, as to workaround out of order loggregator traces displayed (sort by logback timestamps instead of loggregator timestamps)
            - report issue with Paas ops team

    - identify and fix root cause for commit without change list (triggered by CC clean up orphan service instances every 30s)  
           2018-02-05T11:25:46.78+0100 [APP/PROC/WEB/0] OUT 2018-02-05 10:25:46.785  INFO 12 --- [nio-8080-exec-3] c.o.o.c.b.o.o.git.GitProcessor           : [paas-template.] staged commit:  deleted:[] added:[] changed:[]
           
            commit f84fa7b0184124a706a41698950f28a2597a0237
            Author: coa-cassandra-broker <codex.clara-cloud-dev@orange.com>
            Date:   Sun Feb 4 07:27:03 2018 +0000
            
                commit by ondemand broker
            
            commit b22dddf0f1d33df203c29700cc9948ba46e1939a
            Author: coa-cassandra-broker <codex.clara-cloud-dev@orange.com>
            Date:   Sun Feb 4 06:57:27 2018 +0000
            
                Cassandra broker: delete instance id=be8bf409-d64b-4c26-a42b-90a65459d051

           
           
   - refine trace to display additional changes 
           
        if (status.hasUncommittedChanges()) {
            logger.info(prefixLog("staged commit:  deleted:") + status.getRemoved() + " added:" + status.getAdded() + " changed:" + status.getChanged());
            CommitCommand commitC = git.commit().setMessage(getCommitMessage(ctx));

 
    - collapse pushed trace with following details 
                logger.info(prefixLog("pushed ..."));

                   2018-02-05T11:25:47.60+0100 [APP/PROC/WEB/0] OUT 2018-02-05 10:25:47.601  INFO 12 --- [nio-8080-exec-3] c.o.o.c.b.o.o.git.GitProcessor           : [paas-template.] pushed ...
                      2018-02-05T11:25:35.51+0100 [APP/PROC/WEB/0] OUT 2018-02-05 10:25:35.510  INFO 12 --- [nio-8080-exec-2] c.o.o.c.b.o.o.git.GitProcessor           : [paas-template.] Failed to push with status [REJECTED_NONFASTFORWARD]


    In manual scenario
           2018-02-05T11:25:35.66+0100 [RTR/0] OUT coa-cassandra-broker.redacted-domain.org - [2018-02-05T10:25:24.457+0000] "PUT /v2/service_instances/a547bc0e-57f5-4197-9a20-bf3bb2d8f5da?accepts_incomplete=true HTTP/1.1" 500 339 94 "-" "HTTPClient/1.0 (2.8.2.4, ruby 2.4.2 (2017-09-14))" "192.168.26.30:44986" "192.168.27.27:60376" x_forwarded_for:"192.168.99.31, 192.168.26.30" x_forwarded_proto:"https" vcap_request_id:"7ce3a6ef-47c7-4f94-64f9-1d352a2143ec" response_time:11.202739743 app_id:"47214529-8b56-451c-bb36-5bbd68e1cb7a" app_index:"0" x_b3_traceid:"40982f06948ad7ef" x_b3_spanid:"40982f06948ad7ef" x_b3_parentspanid:"-"

   2018-02-05T11:25:48.36+0100 [RTR/0] OUT coa-cassandra-broker.redacted-domain.org - [2018-02-05T10:25:37.351+0000] "DELETE /v2/service_instances/a547bc0e-57f5-4197-9a20-bf3bb2d8f5da?accepts_incomplete=true&plan_id=cassandra-ondemand-plan&service_id=cassandra-ondemand-service HTTP/1.1" 200 0 2 "-" "HTTPClient/1.0 (2.8.2.4, ruby 2.4.2 (2017-09-14))" "192.168.26.30:57040" "192.168.27.27:60376" x_forwarded_for:"192.168.99.31, 192.168.26.30" x_forwarded_proto:"https" vcap_request_id:"780c1484-22f1-4077-4501-540df66c2287" response_time:11.009711297 app_id:"47214529-8b56-451c-bb36-5bbd68e1cb7a" app_index:"0" x_b3_traceid:"e505d0ff00e24e2e" x_b3_spanid:"e505d0ff00e24e2e" x_b3_parentspanid:"-"
 

TransportException

- Release on-demand cassandra bosh deployment
    - automate broker deployment in paas-template from binary in github release
       - deploy latest from develop branch
        

- Smoke tests: create service instance, get service instance (wait for success), delete service instance

- Implement OSB provision delegation to nested cassandra broker
   - refine PipelineCompletionTracker to call OSB client create/delete/bind/unbind
      - introduce OSBProxy
         - pull OsbClientFactory in
            - pb: circular maven dependencies
            - solution: 
               - move back open-service-broker-client classes to cf-ops-automation-broker-core 
                  - run integration tests 
               - remove open-service-broker-client dependences and delete open-service-broker-client   
           
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

