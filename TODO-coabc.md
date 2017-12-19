   
- cassandra processor: 
    - triggers new bosh deployment: writes paas-template/paas-secret
    - tracks completion of bosh deployment: watches rendered manifest file 

- git processor
   - prioritize caching:
        pull --rebase instead of clone
    
    - for paas-template support:
        - integration test: paas-template create feature-COAB-cassandra-IT:
            - disable submodules commands unless a specific key is registered 
        - submodules without opt-in get disabled  
            submodule.active=false # did not manage to see side effect of this config.
            submodule.<name>.update=none
        - submodules get selectively enabled as needed, controlled by a context key  https://git-scm.com/docs/git-config#git-config-submoduleltnamegtactive
            submodule.active=false # did not manage to see side effect of this config.
            submodule.<name>.active=false
            submodule.<name>.update=none
            
        - add support for replicated submodules https://github.com/orange-cloudfoundry/cf-ops-automation/issues/69 maintain a ~/.giconfig of form
             - [url "https://gitlab.internal.paas"]
                   insteadOf = "https://github.com"  
                    
       
    - refactor GitProcessorContext into PreGitProcessorContext and PostGitProcessorContext
        - and prefix keys with in and out
    - Implement failIfRemoteBranchExists key
    - Implement deleteRemoteBranch key
    
       




    
    - refactor git tests to initialize clones in the gitserver setup rather than using the gitprocessor in the test itself

- credhub processor
    - integration test

- osb processor: create service instance 
    - credhub write support
    - waits for a key in context to start in any processorchain method 
        create-service-instance: a spring-cloud-cloudfoundry-service-broker object 
        update-service-instance 
        delete-service-instance 
        create-service-binding 
        delete-service-binding 
        maintains current state in context (for credhub processor to persist it) 
        polls service instance creation 
        pushes response in the context

- core framework: create/delete service binding 

- osb processor: create/delete service binding
