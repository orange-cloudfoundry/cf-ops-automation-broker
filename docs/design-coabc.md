# Global hypothesis
- No evolutions on cf-ops-automation (asis)

- Processor chain : 
   - cassandra processor
   - git processor
   - credhub processor
   - osb processor

- Sequence diagram

Phase 1: shared cassandra branch

create service instance cassandra  (service plan small)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout develop # checkOutRemoteBranch 
                Branch develop set up to track remote branch develop from origin.
                Switched to a new branch 'develop'
            git checkout cassandra || git checkout -b cassandra #createBranchIfMissing 
            
        CassandraProcessor:
            produce files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push -u origin cassandra



update service instance cassandra (service-plan large)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout cassandra #fails if cassandra branch does not exist 
                                   # checkOutRemoteBranch
                                   
        CassandraProcessor:
            update files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push -u origin cassandra


delete service instance cassandra
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout cassandra #fails if cassandra branch does not exist 
            
        CassandraProcessor:
            remove files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push -u origin cassandra




Phase 2: per service instance branch

create service instance cassandra  (service plan small)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout develop 
                Branch develop set up to track remote branch develop from origin.
                Switched to a new branch 'develop'
            git checkout -b service-instance-guid #fails if branch already exist 
            
        CassandraProcessor:
            produce files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push -u origin service-instance-guid



update service instance cassandra (service-plan large)
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout service-instance-guid #fails if service-instance-guid branch does not exist 
            
        CassandraProcessor:
            update files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit 
            git push -u origin service-instance-guid


delete service instance cassandra
    precreate: 
        GitProcessor paas-template:
            git clone paas template
            git checkout service-instance-guid #fails if service-instance-guid branch does not exist 
            
        CassandraProcessor:
            remove files in workdir

    postCreate
        GitProcessor paas-template:
            git add        
            git commit
            git push origin service-instance-guid # push delete commit for audit purposes. 
            git push -u origin :service-instance-guid # delete the branch.
                TODO: check behavior of COA sync-feature-branch





# Git 
- paas-templates :
    - short term : work on a single branch (grouping all service instance depls) called feature-coabdepls-cassandra
    - mid term : work on multiple branches (one branch per cassandra service instance)
        - feature-coabdepls-cassandra_guid1 
        - feature-coabdepls-cassandra_guidn

- paas-secrets :
    - master branch is used

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

# Git processor behaviour

# Credhub processor behaviour

# Osb processor behaviour


