-------------------
Git repo caching

- check behavior of pool w.r.t. two non equal context objects. 
   - Do we really need the Key in the pool ?
   - Instead store a Git or Context object (git, workdir...) ?
     - Git: missing workDir to delete to ? 
     - Context: awkward to pool the whole context which might include other objects and create memory leak
     - a new object with needed info the the fetch & refresh ?
     
     => implement gitFetchAndReset() 
     
- refine error handling  
    - add diagnostic logs before rethrowing Exception
    - clear pooled entries ?
 
Q: rename PooledGitManager into SimpleGitProcessor ?
- Modify clients to instanciate PooledGitManager
- PooledGitManager: delegates commitPush to gitManager
- PooledGitManager: tunes the pool (max size, ...)
- PooledGitManager: expose metrics for operability (pool stats)
- PooledGitRepoFoctory: validates pooled git repo through a git pull & git reset
- PooledGitRepoFoctory: un/passivate pooled git repo by renaming their dir ? 


Reqs:
    - operability: be able to distinguish (from disk or else from logs) cache entries from actively used clones
    - cache entries gets discarded when disk full reached upon fetch/clone

Option 1: add AOP/spring caching behavior
         https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache
         Pb: does not map well a caching abstraction, this is rather a pool abstraction
         
         Same with https://github.com/ben-manes/caffeine even with http://static.javadoc.io/com.github.ben-manes.caffeine/caffeine/2.6.2/com/github/benmanes/caffeine/cache/RemovalListener.html

Option 2: look for an off-the-shelf pooling cache component + refactor Processor


Matching pooling libraries/concepts
    - https://commons.apache.org/proper/commons-pool/api-2.6.0/index.html

Q: do we need to distinguish pooled repos (e.g. remote URI, remote branch, submodules fetched) ?
A: 
- both paas-secret and paas-template URIs should be fetched, with distinct remote branches
- however the git repo uri is provided to the GitProcessor constructor  
+ we instead refresh pooled repos each time we take them from the pool (git fetch + git reset) 

```
public interface PooledObjectFactory<T> {
    PooledObject<T> makeObject();
    void activateObject(PooledObject<T> obj);
    void passivateObject(PooledObject<T> obj);
    boolean validateObject(PooledObject<T> obj);
    void destroyObject(PooledObject<T> obj);
}
```

Q: how do we pass in the context keys to the Factory impl ?
- introduce keep one pool per key
- use KeyedPooledObjectFactory<K,V> instead https://commons.apache.org/proper/commons-pool/api-2.6.0/org/apache/commons/pool2/KeyedPooledObjectFactory.html   


First experiment: does it make sense to extract GitCloner ?
- GitProcess becomes anemic
- GitProcessorTest 
    - does white box testing (protected methods) 
    - set up using clone/commit implies coupling to GitCloner/GitPusher 

Deadcode:

processor.configureCrLf(repository.getConfig());


-------------------
Better error handling

     19:38:41.629: [APP/PROC/WEB.0] Caused by: org.eclipse.jgit.errors.TransportException: No space left on device
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.BasePackFetchConnection.doFetch(BasePackFetchConnection.java:376) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.TransportHttp$SmartHttpFetchConnection.doFetch(TransportHttp.java:1090) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.BasePackFetchConnection.fetch(BasePackFetchConnection.java:303) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.BasePackFetchConnection.fetch(BasePackFetchConnection.java:292) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.FetchProcess.fetchObjects(FetchProcess.java:246) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.FetchProcess.executeImp(FetchProcess.java:162) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.FetchProcess.execute(FetchProcess.java:123) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.transport.Transport.fetch(Transport.java:1236) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]
     19:38:41.629: [APP/PROC/WEB.0] 	at org.eclipse.jgit.api.FetchCommand.call(FetchCommand.java:239) ~[org.eclipse.jgit-4.9.0.201710071750-r.jar!/:4.9.0.201710071750-r]





#30 Full COA conventions support:
- [DONE]set COA rules as constants (in DeploymentConstants?) instead of configuration flags:
   - https://github.com/orange-cloudfoundry/cf-ops-automation#ops-files
    > By convention, all files in template dir matching *-operators.yml are used by bosh-deployment as ops-files inputs.
- [DONE]remove prereq to have a file `operators/coab-operators.yml` present but still preserve symlinking it
   - it is fine to have a deployment without any operators file
- add systematic symlink of any template/ file 
   - [DONE]1st step: ignoring subdirs, regardless of their name if their present as file in this directory,
   - [DONE]2nd step: also mirror the subdirs with nested symlinks to support iaas-specific templates
- [DONE]refine TemplatesGeneratorTest.populatePaasTemplates to make bdd style integration test:
* [DONE]given a reference model in the src/test/resources such as 
```
coab-depls/mongodb
|-- deployment-dependencies.yml
`-- template
    |-- coab-operators.yml
    |-- add-prometheus-operators.yml
    |-- add-shield-operators.yml
    |-- mongodb-vars.yml
    `-- mongodb.yml
```
* and a user request 
* when generating the service instance
* then the service instance looks like

```
$tree coab-depls/m_f49911f7-b69a-4aba-afdf-23fd11014278

coab-depls/m_f49911f7-b69a-4aba-afdf-23fd11014278
|-- deployment-dependencies.yml -> ../../template/mongodb/deployment-dependencies.yml
`-- template
    |-- coab-operators.yml -> ../../mongodb/operators/coab-operators.yml
    |-- mongodb-vars.yml -> ../../mongodb/template/mongodb-vars.yml
    |-- m_f49911f7-b69a-4aba-afdf-23fd11014278.yml -> ../../mongodb/template/mongodb.yml
    `-- coab-vars.yml
```
[DONE]- update TemplatesGeneratorTest.aModelStructure() to use the src/test/resources/sample-deployment-model/coab-depls/mongodb or a distinct dedicated deployment model
    + consider removing assertXX() methods redundant with populatePaasTemplates() 
[DONE]- Update BoshServiceProvisionningTest#initPaasTemplate() to use file copy as well instead of crafting individual files explicitly
[ONGOING/GB]- convert parasite configurable properties into constants in DeploymentProperties
    private String secrets = "secrets"; //Secrets directory (i.e secrets)
    private String meta = "meta"; //Meta directory (i.e meta)
    private String template = "template"; //Template directory (i.e template)
    private String vars = "vars"; //Vars suffix (i.e vars)
    private String operators = "operators"; //Operators suffix (i.e operators)

- remove extra checks that should be valided by getting the deployment model green. Only validate configuration errors in the broker matching relevant DeploymentProperties  
    public static final String ROOT_DEPLOYMENT_EXCEPTION = "Root deployment directory doesn't exist at: ";
    public static final String MODEL_DEPLOYMENT_EXCEPTION = "Model deployment directory doesn't exist at: ";
    public static final String TEMPLATE_EXCEPTION = "Template directory doesn't exist at: ";
    public static final String OPERATORS_EXCEPTION = "Operators directory doesn't exist at: ";
    public static final String MANIFEST_FILE_EXCEPTION = "Model manifest file doesn't exist";
    public static final String VARS_FILE_EXCEPTION = "Model vars file doesn't exist";
    public static final String COAB_OPERATORS_FILE_EXCEPTION = "Coab operators file doesn't exist";
    public static final String SECRETS_EXCEPTION = "Secrets directory doesn't exist at: ";
    public static final String META_FILE_EXCEPTION = "Model meta file doesn't exist";
    public static final String SECRETS_FILE_EXCEPTION = "Model secrets file doesn't exist";
    
- reconsider whether checks are necessary prior to delete service instances, as this may prevent cleaning up invalid/deprecated/N-1 models (i.e. a service instance branch with missing models)
 
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 2018-07-05 21:50:27.547  INFO 8 --- [nio-8080-exec-3] o.o.ProcessorChainServiceInstanceService : Unable to delete service with request DeleteServiceInstanceRequest(super=AsyncServiceInstanceRequest(super=ServiceBrokerRequest(cfInstanceId=null, apiInfoLocation=my-api.com/v2/info, originatingIdentity=Context(platform=cloudfoundry, properties={user_id=0d02117b-aa21-43e2-b35e-8ad6f8223519})), asyncAccepted=true), serviceInstanceId=ac4895ca-8021-4f6c-96a0-cd3915c9fa0f, serviceDefinitionId=mongodb-ondemand-service, planId=mongodb-ondemand-plan), caught com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentException: Model deployment directory doesn't exist at: /home/vcap/tmp/broker-4671928892822661429/coab-depls/mongodb
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.DeploymentException: Model deployment directory doesn't exist at: /home/vcap/tmp/broker-4671928892822661429/coab-depls/mongodb
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.StructureGeneratorImpl.checkThatModelDeploymentExists(StructureGeneratorImpl.java:37) ~[cf-ops-automation-broker-core-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.StructureGeneratorImpl.checkPrerequisites(StructureGeneratorImpl.java:24) ~[cf-ops-automation-broker-core-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.SecretsGenerator.checkPrerequisites(SecretsGenerator.java:25) ~[cf-ops-automation-broker-core-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.BoshProcessor.preDelete(BoshProcessor.java:123) ~[cf-ops-automation-broker-core-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain.delete(ProcessorChain.java:123) ~[cf-ops-automation-broker-framework-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.deleteServiceInstance(ProcessorChainServiceInstanceService.java:91) ~[cf-ops-automation-broker-core-0.27.0-SNAPSHOT.jar!/:0.27.0-SNAPSHOT]
   2018-07-05T23:50:27.54+0200 [APP/PROC/WEB/0] OUT 	at org.springframework.cloud.servicebroker.controller.ServiceInstanceController.deleteServiceInstance(ServiceInstanceController.java:146) [spring-cloud-cloudfoundry-service-broker-1.0.2.RELEASE.jar!/:na]
 

[DONE]Q: how to recursively copy files & preserving symlinks ?

[DONE]Q: how to assert expected dir ?
* using string representation of the directory
   * execute tree command
   * find a java-based tree impl
[DONE]* commit the expected output in the reference dataset & compare
   * find java compare directory tree 
      * including symlinks
      * including coab-vars content 

[TODO]secrets generation like template generation
   - add systematic symlink of any secrets/ file 
   - regardless of their name if their present as file in this directory,

[DONE] https://github.com/orange-cloudfoundry/cf-ops-automation/issues/150
    - [DONE]deployment-dependencies.yml generation as symlink
    - [DONE]enable symlink generation instead of file generation     
    - [DONE]clean useless code (file generation)

[DONE]
   - investigate intellij/maven handling of symlinks in resources and workarounds:
   - [DONE]consider moving reference data set from cf-ops-automation-broker/cf-ops-automation-broker-core/src/test/resources/sample-deployment-model/coab-depls/cf-mysql/template/cf-mysql.yml vers cf-ops-automation-broker/cf-ops-automation-broker-core/sample-deployment-model/coab-depls/cf-mysql/template/cf-mysql.yml 
    how to get to sources ? 
        current dir
        relative to classapth  target/src/resources/../../../sample-deployment-model   
   - [DONE] investigate bug on circle CI when I introduce an empty directory (cf-mysql-deployment) in cf-mysql sample-deployment
    No empty directory. A sigle file .keep filtered by the Copy.java class




- Bump jackson to 2.9.2 or later and pull https://github.com/FasterXML/jackson-dataformats-text/tree/master/yaml



- gracefully log case when operation state missing from Request? 
 
java.lang.NullPointerException: null
	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.PipelineCompletionTracker.getDeploymentExecStatus(PipelineCompletionTracker.java:55) ~[classes/:na]
	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.BoshProcessor.preGetLastOperation(BoshProcessor.java:74) ~[classes/:na]

       

- bump cloudflare version in paas-template
- Generalization
   - Unify TF and Bosh service provisionning and service binding interfaces so that they can be mixed
   - Refactor:
        - regroup splitted assertions in a single method for each step: generate, check, remove


- Confirm deprovision errors are now ignored and deployment proceeds

 2018-03-28T17:58:58.27+0200 [APP/PROC/WEB/0] OUT 2018-03-28 15:58:58.271  INFO 13 --- [nio-8080-exec-7] .o.o.c.b.o.o.p.PipelineCompletionTracker : Unable to delegate delete to enclosed broker, maybe absent/down. Reporting as GONE. Caught:java.lang.IllegalArgumentException: unexpected url: https://cassandra-brokercassandravarsops_febaea1a-aaac-4266-9a9b-56506eacb356..../v2/catalog

        external_host: cassandra-brokercassandravarsops_febaea1a-aaac-4266-9a9b-56506eacb356.((!/secrets/cloudfoundry_system_domain))

        

- Current limitation: COA does not trigger update of service instances when template is modified
   - Because service instances get resource don't see symlinked changes  

      
- Understand why smoke tests sometimes fails abrutly before async service creation polling timeout. e.g. build 396 Suspecting set +x obscure exit in while loop

- Fix symptoms from collected  traces:

  2018-03-28T07:07:57.06+0000 [APP/PROC/WEB/0] OUT 2018-03-28 07:07:57.059 ERROR 7 --- [nio-8080-exec-7] c.o.o.c.b.o.o.git.GitProcessor           : [paas-template.] unable to clean up /home/vcap/tmp/broker-7549514514408811704





- Refine tarball automatic deployment to filter on the current cassandra PR branch name to avoid deploying old version
   - investigate an additional "branch" filter accepted on the artifact endpoint: https://circleci.com/api/v1.1/project/github/orange-cloudfoundry/cf-ops-automation-broker/latest/artifacts?filter=successful 


- Improve diagnostics when the requested service instance paas-template does not get merged by the pipeline
   - checkout a paas-template clone 
   - feedback to end user "pending deployment" vs "deployment started"
   - add debugging traces 


- Update README.md 
   - add TOC
   - Provide status about cassandra and cloudflare brokers
   - move framework internals to design.md
   - move contribution section to contributing.md
  


- Bump to springboot 2.0
    - Use javax bean validation 2.0 in VarsFilesYmlFormatter for validating maps  
- Bump to spring cloud service broker 2.0
- Upgrade to mockito 2
- Upgrade to junit5 to benefit from nested junit classes as well as descrptive names https://junit.org/junit5/docs/5.0.1/user-guide/
   - https://medium.com/@dSebastien/using-junit-5-with-spring-boot-2-kotlin-and-mockito-d5aea5b0c668
   - https://github.com/mockito/mockito/issues/445
   - https://github.com/mockito/mockito/issues/1348


- future bind request mapping improvements
   - factor out plan mapping into a method and then a bean 
   - support route binding responses                 


- improve coab concourse integration
    - schedule smoke test service instance deletion automatic approval 
        login -t micro.preprod -c url  -u atc -p password ;  /usr/sbin/fly -t micro.preprod trigger-job --job=coab-depls-generated/approve-and-delete-disabled-deployments) 
    - trigger coab deployment by watching circle ci tar balls    
        
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
   2018-02-28T10:26:59.37+0100 [APP/PROC/WEB/0] OUT     at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.BoshProcessor.preGetLastOperation(CassandraProcessor.java:71) ~[cf-ops-automation-broker-core-0.25.0-SNAPSHOT.jar!/:0.25.0-SNAPSHOT]
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
 

- add support for returning templated dashboard in Cassandra processor provision responses    


- core framework: update service 


--------------------------------
   
   
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
     
- credhub 
   - write support
   - integration test
      - record wiremocks 
         - using tunnels https://starkandwayne.com/blog/accessing-bosh-and-credhub-via-magic-tunnels/




