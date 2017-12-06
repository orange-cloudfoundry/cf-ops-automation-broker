
# Global hypothesis
- Processor chain: 
   - cassandra processor
   - git processor
   - credhub processor
   - osb processor

- Sequence diagram


# Concourse/cf-ops-automation evolutions

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


