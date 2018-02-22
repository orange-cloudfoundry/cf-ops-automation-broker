# Global hypothesis
- No evolutions on cf-ops-automation (as-is)

- Processor chain : 
   - static git repo selector processor: specifies expected git repos, and branches
   - dynamic git repo selector processor: specifies expected git repos, and branches (from OSB service instance guid)
   - cassandra deployment templates processor: generates/updates/deletes files following create/update/delete OSB key in context
   - bosh deployment completion processor: 
      - observe bosh deployment completion (through manifest.yml)
      - write OSB failed async create/update/delete response on timeout
   - cassandra service broker processor: maps ingress OSB request to egress OSB requests  
   - git processor
   - credhub processor
   - sync osb processor: make sync OSB 
   - osb async operation context persistence processor: mux/demux state (creation time, request) into OSB operation field

# Git

## Overview
- paas-templates :
    - short term : work on a single branch (grouping all service instance depls) called feature-coabdepls-cassandra
    - mid term : work on multiple branches (one branch per cassandra service instance)
        - feature-coabdepls-cassandra_guid1 
        - feature-coabdepls-cassandra_guidn

- paas-secrets :
    - master branch is used

## Paas-template submodule handling

- how to fetch github modules ?
  - Q: add support for http proxy for fetching github submodules ? 
      - JGit supports environment variables
                String keyNoProxy = "no_proxy";
                String keyHttpProxy = "http_proxy";
                String keyHttpsProxy = "https_proxy";
      - Q: what other http resources would proxy interfere with ?
         - OSB processor: IP is dynamically provided, so can't rely on static no_proxy env var unless using fragile no-proxy regexp syntax
             - so we'd need to programmatically assign system.properties, with append only, reducing likelyhood of race conditions
         - private gitsubmodule
  - A: No, rather focus on replicated submodules https://github.com/orange-cloudfoundry/cf-ops-automation/issues/69 maintain a ~/.giconfig of form:
  
    ```  
           [url "https://gitlab.internal.paas"]
               insteadOf = "https://github.com"  
    ```  

     - by default submodules are disabled and get selectively enabled as needed  https://git-scm.com/docs/git-config#git-config-submoduleltnamegtactive
     
    ```  
            submodule.active=false
            submodule.<name>.active=true 
    ```


## sequence diagram of processor interactions

### Git and Cassandra Processor

#### Phase 1: shared cassandra branch

create service instance cassandra  (service plan small)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout develop # checkOutRemoteBranch 
                Branch develop set up to track remote branch develop from origin.
                Switched to a new branch 'develop'
            ~~git checkout cassandra || git checkout -b cassandra && git branch -u origin/cassandra #createBranchIfMissing~~ 
            ~~git checkout cassandra || git branch cassandra -u origin/cassandrag && git checkout cassandra #createBranchIfMissing~~ 
            ~~(git branch --list cassandra || git branch cassandra -u origin/cassandra)  && git checkout cassandra #createBranchIfMissing~~ 
            git branch cassandra ; git config branch.cassandra.remote origin; git config branch.cassandra.merge refs/heads/cassandra; git checkout cassandra   #createBranchIfMissing 
            
        CassandraProcessor:
            produce files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push 



update service instance cassandra (service-plan large)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout cassandra # fails if remote cassandra branch does not exist 
                                   # checkOutRemoteBranch
                                   
        CassandraProcessor:
            update files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push 


delete service instance cassandra
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout cassandra #fails if remote cassandra branch does not exist 
                                   # checkOutRemoteBranch
            
        CassandraProcessor:
            remove files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push 




#### Phase 2: per service instance branch

create service instance cassandra  (service plan small)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout develop 
                Branch develop set up to track remote branch develop from origin.
                Switched to a new branch 'develop'
                
                # failIfRemoteBranchExists: 
            git branch -rl service-instance-guid #fails if branch already exist 


                # createBranchIfMissing                            
            git branch service-instance-guid ; git config branch.service-instance-guid.remote origin; git config branch.service-instance-guid.merge refs/heads/service-instance-guid; git checkout service-instance-guid   
            
            
        CassandraProcessor:
            produce files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push 



update service instance cassandra (service-plan large)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout service-instance-guid #fails if service-instance-guid branch does not exist 
                                               # checkOutRemoteBranch
        CassandraProcessor:
            update files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push


delete service instance cassandra
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout service-instance-guid #fails if service-instance-guid branch does not exist 
                                               # checkOutRemoteBranch
        CassandraProcessor:
            remove files in workdir

    postCreate:
        GitProcessor paas-template:
            git add        
            git commit
            git push # push delete commit for audit purposes.
            
            git push :service-instance-guid # delete the branch.
                Note: currently prevented by https://github.com/orange-cloudfoundry/cf-ops-automation/issues/67
                     # deleteRemoteBranch






# Concourse/cf-ops-automation evolutions/pipelines (coab-depls)
- new root deployment/pipeline introduced called coab-depls targeting BOSH-OPS 
    - ruby create-root-depls.rb -d coab-depls -t /home/ijly7474/GIT/paas-templates -p /home/ijly7474/GIT/bosh-cloudwatt-secrets-pprod
    - /!\Destroy paas-secrets/shared/secrets.yml file
    - Update [paas-secrets/coab-depls/ci-deployment-overview.yml] file for configuring new pipelines
    - Update [paas-secrets/micro-depls/credentials-auto-init.yml] file for setting new entries for coab
    - Update [paas-templates/coab-depls/coab-depls-versions.yml] file for setting stemcell name and version
    - Update [paas-templates/coab-depls/template/deploy.sh] file for stemcell name and version
    - Update [paas-templates/coab-depls/template/cloud-config-tpl.yml] file (target BOSH-ONDEMAND) -> symlink  
    - Update [paas-templates/coab-depls/template/runtime-config-tpl.yml] file (target BOSH-ONDEMAND) -> symlink 


    - Update [paas-secrets/coab-depls/secrets/meta.yml] file
    - Update [paas-secrets/coab-depls/secrets/secrets.yml] file

    TO UPDATE


- encountered problems
    - resource 'bosh-stemcell' is not used => https://elpaaso-concourse-micro.redacted-domain.org/teams/main/pipelines/coab-depls-init-generated/jobs/update-pipeline-coab-depls/builds/14
    =>Solution = deployment model cassandra

    - in secrets, it is not possible to link towards deployment directory ( cp: cannot overwrite directory 'result-dir/./coab-depls/cassandra#aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0' with non-directory)
    ln -s ../ops-depls/cassandra#aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0 cassandra#aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0 => https://elpaaso-concourse-micro.redacted-domain.org/teams/main/pipelines/coab-depls-init-generated/jobs/update-pipeline-coab-depls/builds/16
    =>Solution = symlinks towards files    

    - concourse uses up todate versions : 
    needs to trigger manually the init-concourse-boshrelease-and-stemcell-for-coab-depls(known issue)
    =>Solution = manual action on Concourse (one shot)    

    - pipeline is not triggered automatically the first time it is instancied (known issue)
    needs to trigger manually update-pipeline-coabl-depls-generated 
    =>https://github.com/orange-cloudfoundry/cf-ops-automation/issues/29
    =>Solution = manual action on Concourse (one time), concourse processor or fake commit ?       

    - no # in deployment name (Bosh manifest) => HTTP 404 returned by director
    =>Issue to raise
    
    - no @ in deployment name (Bosh manifest) => HTTP 500 returned by director
    =>Issue to raise 

    - route registrar 
        curl -X GET https://cassandra-brokerc.redacted-domain.org:443/v2/catalog
        404 Not Found: Requested route ('cassandra-brokerc.redacted-domain.org') does not exist.
    =>To be investigated       

Specify the template to use for an on-demand deployment:
- coab writes to paas-templates via git
  - problems : 
    - paas-templates is shared with other instances (PROD/PPROD)
    - current prod concourse doesn't yet include feature branch merge pipelines
    - merge pipeline is often red in preprod. Will hold coab bosh trigger 
  - solutions : hidden branch
- cf-ops-automation modified to trigger bosh deployment pipelines not matching paas-templates structure
    - enable-deployment.yml support in paas-secret is enhanced:

```yml
enable-deployment.yml:
   # optional relative path to bosh definition directory in paas-template (typically holding bosh template)
   # if missing, then defaults to `pwd` (i.e. current directory relative path in paas-secret)
   - definition_directory_path: /ops-depls/cassandra
```

   - per deployment bosh ops files and vars files are processed
     
├── coab-depls
│   ├── cassandra-{guid}
│   │   ├── cassandra-{guid}.yml
│   │   ├── enable-deployment.yml
│   │   └── secrets
│   │       ├── meta.yml
│   │       └── secrets.yml
│   │   └── specs
│   │       ├── deployment-name-operator.yml
│


# Repository, directories and files description
- target git repository is secrets (preprod-secrets/bosh-cloudwatt-secrets)
- root directory in secrets is (coab-depls)
- each cassandra on coab-depls is stored in a dedicated directory named (cassandra-{guid}))
- handled files <b>?stored in COAB FS?</b>: 
    - [secrets/input written by coab]activation file => /coab-depls/cassandra-{guid}/enable-deployment.yml (in)
    - [secrets/input written by coab]secret file => /coab-depls/cassandra-{guid}/secrets/secrets.yml
    - [secrets/input written by coab]meta file => /coab-depls/cassandra-{guid}/secrets/meta.yml
    - [secrets/output]manifest file => /coab-depls/cassandra-{guid}/cassandra-{guid}.yml

    - [templates/input]manifest file => /cassandra-depls/cassandra-{guid}/template/cassandra.yml (symlink)
    - [templates/input]ops file for changing deployment name => /cassandra-depls/cassandra-{guid}/template/deployment-name-operator.yml (in)

- decisions templating location
    - shared static templates
        - symbolic link to unique template (paas-templates) 
           - pros: 
              - preserves paas-template logic/packaging mechanism 
              - shared updates within service instances
           - cons: 
        - duplication in each service instance
           - pros
           - cons
    - per service instance interpolated (ops file including service instance guid)
        - COAB templating: favored for generic ops files common to all services: deployment name
        - TF templating
        - Spruce templating
    
    


#Credhub entries 
- root path is /cassandra-depls/cassandra-{guid} 
    - [password]/cassandra-depls/cassandra-{guid}/cassandra_admin_password
    - [password]/cassandra-depls/cassandra-{guid}/cassandra_key_store_pass
- secrets path is /secrets/
    - [value]/secrets/cloudfoundry_system_domain
    - [password]/secrets/cloudfoundry_admin_password    
    - [value]/secrets/cloudfoundry_apps_domain



# Context keys

# Cassandra processor behaviour

CC -> COAB:cassandra-processor PUT service_instance/guid (create-service instance) https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#provisioning
CC <- COAB:cassandra-processor 
                requests a new bosh deployment through TemplateGenerator/SecretsGenerator
                puts create-service-instance-response in context: 
                    202 accepted 
                    return operation: 
                        {"lastOperationDate":"2017-11-14T17:24:08.007Z","operation":"create"})         
                        // Extract from logic from TerraformCompletionTrackerTest )
                clears create-service-instance-request from context        
           git-processor commit & push, triggering asynchronously in concourse:
             
               
CC -> COAB:cassandra-processor GET https://user:password@cassandra-guid-brokerURL/service-instance/get-last-operation
           read state from credub:
               - key = cassandra_deployment_guid_service_instance_provisionning value: json with
                  - state= bosh 
            
           observes bosh manifest in paas-secret to infer bosh deployment success,
                if bosh deployment success, 
                    inserts create-service-instance-request in context with values:
                        same service instance guid
                        map service plan. either
                            hardcoded in cassandra service broker manifest.yml 
                            (later) ~~fetched from OSB processor catalog response~~
                        propagate arbitrary params (e.g. cassandra-replication-factor) 
                            (except ones driving bosh release: replicated-into-data-center=CW-FR2 )
                        same context/org/space
                    updates it state into credub:
                        - key = cassandra_deployment_guid_service_instance_provisionning value: json with
                           - state= creating 
           otherwise timeout if elapsed w.r.t. operation.lastOperationDate > configured timeout
           
           
COAB:osb-processor -> cassandra-broker:  PUT service_instance/guid (create-service instance) 
COAB:osb-processor <- cassandra-broker:  201 Created

CC -> COAB: get last operation https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#polling-last-operation
    osb-processor -> cassandra-broker:  get last operation

## PipelineCompletionTracker design

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


# Git processor behaviour

# Credhub processor behaviour

# Osb Proxy

The Osb client isn't following the processor model and is instead a POJO invoked by CassandraProcessor

- Right now, CassandraProcessor handles delete asynchronous, whereas it could have been synchronous:
        - we could synchronously execute osbDelegate.unprovision() + delete manifest and return sync successfull delete
        - However, this makes it asymetrical and more work
        - May be useful in the future when we will really need async delete support with COA
            - when refactoring and merging with TF modules
            - when/if COA implements sync deprovision 

# faq

## Why delegating provisionning nested broker call within outter provision call (and not binding)

A: this enables COAB to delegate the full provision request without requiring statefull support.

provisionning request params:
* [x] service_id* 	string 	MUST be the ID of a service from the catalog for this Service Broker.
* [x] plan_id* 	string 	MUST be the ID of a plan from the service that has been requested.
* [x] context 	object 	Platform specific contextual information under which the Service Instance is to be provisioned. Although most Service Brokers will not use this field, it could be helpful in determining data placement or applying custom business rules. context will replace organization_guid and space_guid in future versions of the specification - in the interim both SHOULD be used to ensure interoperability with old and new implementations.
* [x] organization_guid* 	string 	Deprecated in favor of context. The Platform GUID for the organization under which the Service Instance is to be provisioned. Although most Service Brokers will not use this field, it might be helpful for executing operations on a user's behalf. MUST be a non-empty string.
* [x] space_guid* 	string 	Deprecated in favor of context. The identifier for the project space within the Platform organization. Although most Service Brokers will not use this field, it might be helpful for executing operations on a user's behalf. MUST be a non-empty string.
* [ ] parameters 	object 	Configuration options for the Service Instance. Service Brokers SHOULD ensure that the client has provided valid configuration parameters and values for the operation.

Extract of https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object
| Request API | Properties | | --- | --- | --- | 
| PUT /v2/service_instances/:instance_id | organization_guid, space_guid | 
| PATCH /v2/service_instances/:instance_id | organization_guid, space_guid | 
| PUT /v2/service_instances/:instance_id/service_bindings/:binding_id | organization_guid, space_guid |

cf cs cassandra -c '{"keyspacename"="mykeyspace"}'
