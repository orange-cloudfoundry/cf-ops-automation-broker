
- Fix BoshServiceProvisionningTest
- Restore assertion in SecretsGenerator.check_that_enable_deployment_file_is_generated_collocated_with_coa_produced_manifest


- rename brokers to more generic names
   - rename jars
   - search for cassandra specifics: 
      - CassandraProcessorConstants 
   - search for cloudflare specifics: see  
      - //FIXME: cloudflare specifics to be moved out
      - //FIXME: cloudflare specific default to be changed
      - config: route-suffix 
      - arbitrary param: route
         - + unicity of this param
   
   - make a new release
      - register in bintray new project
   - update jars in paas-template

- bump cloudflare version in paas-template
- Generalization
   - Unify TF and Bosh service provisionning and service binding interfaces so that they can be mixed


- Long term fix for regression following varops template introduction: provisionning always fail with a timeout because it's waiting for the manifest at the wrong path

    PipelineCompletionTracker uses 
    ```java
            public Path getTargetManifestFilePath(Path workDir, String serviceInstanceId) {
                return StructureGeneratorHelper.generatePath(workDir,
                            CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                            CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                            CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.YML_SUFFIX);
            }
    ```
        
    PipelineCompletionTrackerTest uses:
    
    ```java
        private void generateSampleManifest() throws IOException {
            Path serviceInstanceDir = StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID);
            serviceInstanceDir = Files.createDirectories(serviceInstanceDir);
            Path targetManifestFile = StructureGeneratorHelper.generatePath(serviceInstanceDir,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + SERVICE_INSTANCE_ID + CassandraProcessorConstants.YML_SUFFIX);
            Files.createFile(targetManifestFile);
        }
      
    ```
    
        SecretsGenerator uses:
        
        ```java
            
                        //Compute instance directory
                        String deploymentInstanceDirectory = this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId;
            
                        //Generate secrets directory
                        StructureGeneratorHelper.generateDirectory(workDir, this.rootDeployment, deploymentInstanceDirectory, this.secrets);
    
        ```
    
            where modelDeployment is injected by CassandraBrokerApplication from DeploymentProperties 
    
        + CassandraServiceProvisionningTest don't detect this, as it also uses PipelineCompletionTracker
    
        ```java
            public void simulateManifestGeneration(GitProcessor gitProcessor) throws IOException {
                Context context = new Context();
                gitProcessor.preCreate(context);
        
                Path workDirPath = (Path) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString());
                @SuppressWarnings("unchecked") PipelineCompletionTracker tracker = new PipelineCompletionTracker(Clock.systemUTC(), osbProxyProperties.getMaxExecutionDurationSeconds(), Mockito.mock(OsbProxy.class));
                Path targetManifestFilePath = tracker.getTargetManifestFilePath(workDirPath, SERVICE_INSTANCE_ID);
                createDir(targetManifestFilePath.getParent());
                createDummyFile(targetManifestFilePath);
        
                gitProcessor.postCreate(context);
            }
        ```
    
            
        Possible fixes:
        - **workaround** transiently hardcode the manifest path unrelated to the deployment model
        - use the same code to generate deployment files in secrets than to watch for concourse completion ion deployment dir, and share this code with test.
            - refactor PipelineCompletionTracker + CassandraServiceProvisionningTest to delegate the ManifestPath computing to SecretsGenerator
               - extract code from SecretsGenerator:  
               
                  /**
                    * Provide path where concourse should generate the manifest once the deployment is complete
                    * @param workDir The local checkout of paas-secret git repo
                    * @param serviceInstanceId the service instance guid
                    */
                   public Path getDeploymentInstancePath(Path workDir, String serviceInstanceId) {
                       String deploymentInstanceDirName = getDeploymentInstanceDirName(serviceInstanceId);
                       //Compute path
                       return StructureGeneratorHelper.generatePath(workDir, this.rootDeployment, deploymentInstanceDirName, this.secrets);
                   }
               
                   String getDeploymentInstanceDirName(String serviceInstanceId) {
                       return this.modelDeployment + DeploymentConstants.UNDERSCORE + serviceInstanceId;
                   }
     
                 Drawbacks: SecretsGenerator contructor would pull lots of unnecessary to PipelineCompletionTracker, making unit tests heavier
                 Solution:
                    - Extract interface: SecretsReader, ServiceInstanceDeploymentBuilder ...  
    
    
    
        Q: why are so many flags exported by DeploymentProperties ? Would all be potentially configured by the COAB operator ?
        Q: why is StructureGeneratorImpl.generate not abstract and subclassed many times ? 
     

    
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





- Continue generalization of vars/ops
   - replace deployment_name with service-instance-guid
   - add service plan name
   - add space_guid,org_guid, originating_user


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
  


- harden smoke tests:
   - to ease CF/concourse correlation: rename service instance with the the service instance guid
   - refactor to use more readeable syntax: trap, seq, functions, see 
      - https://github.com/cloudfoundry/capi-release/blob/develop/src/capi_utils/monit_utils.sh
      - https://github.com/cloudfoundry/cf-deployment-concourse-tasks/blob/master/bosh-delete-deployment/task
      

- Bump to springboot 2.0
- Bump to spring cloud service broker 2.0
- Upgrade to mockito 2
- Upgrade to junit5 to benefit from nested junit classes as well as descrptive names https://junit.org/junit5/docs/5.0.1/user-guide/
   - https://medium.com/@dSebastien/using-junit-5-with-spring-boot-2-kotlin-and-mockito-d5aea5b0c668
   - https://github.com/mockito/mockito/issues/445
   - https://github.com/mockito/mockito/issues/1348



- complete refactoring of  CassandraServiceProvisionningTest to use OSB client instead of raw rest assured:CassandraServiceProvisionningTest rest-assured based client which is not compliant w.r.t. "X-Broker-API-Originating-Identity" mandatory header.
               
                                   
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
     
- credhub 
   - write support
   - integration test
      - record wiremocks 
         - using tunnels https://starkandwayne.com/blog/accessing-bosh-and-credhub-via-magic-tunnels/




