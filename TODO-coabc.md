   
- cassandra processor: 
    - triggers new bosh deployment: writes paas-template/paas-secret
        - checks prerequisites (root deployment, model deployment, ...)
        - generates paas-secrets structure (directory and files)
        - generates paas-template structure (directory, files and symbolic links) and adapts content
        - improves tests readability (JUnit rules)
        - improves tests coverage (file content)
    
    - tracks completion of bosh deployment: watches rendered manifest file 
    - IT with paas-template/paas-secret: Inspired from GitIT
    - (later once broker password are specific to each instance): specify credhub keys to fetch broker password 
       - woarkaround: broker password passed to coab as env in its manifest.yml

- cassandra service broker:
    - inspired from CloudFlareBrokerApplication
    - CassandraServiceProvisionningTest inspired from CloudFlareServiceProvisionningTest. Potentially simulating concourse observable side effects in git, by driving the embedded GitServer

- git processor
   - disable submodules commands unless a specific key is registered
      - simulate available submodules in local volatile repo & assert they are not present in clone
      - simulate unavailable submodules in local volatile repo: duplicate/share paas-template test fixture with GitIt
         - test case accesses GitServer , and ask to set up a repo with an init step
            - pb: Jgit submodule add fails if the repo is unreacheable
            - solutions:
               - set up a fake nested git repo first, and point it in the path argument: 
               ```
               from git submodule --help
               
                          This requires at least one argument: <repository>. The optional argument <path> is the relative location for the cloned submodule to exist in the superproject. If <path> is not
                          given, the "humanish" part of the source repository is used ("repo" for "/path/to/repo.git" and "foo" for "host.xz:foo/.git"). The <path> is also used as the submodule's logical name
                          in its configuration entries unless --name is used to specify a logical name.
               

                          <path> is the relative location for the cloned submodule to exist in the superproject. If <path> does not exist, then the submodule is created by cloning from the named URL. If
                          <path> does exist and is already a valid Git repository, then this is added to the changeset without cloning. This second form is provided to ease creating a new submodule from
                          scratch, and presumes the user will later push the submodule to the given URL.
               ```
                  - create directory
                  - invoke git init inspiring from GitServer
                  - add the submodule: this fails with 
                        org.eclipse.jgit.api.errors.JGitInternalException: Destination path "bosh-deployment" already exists and is not an empty directory
                            at org.eclipse.jgit.api.CloneCommand.verifyDirectories(CloneCommand.java:254)
                            at org.eclipse.jgit.api.CloneCommand.call(CloneCommand.java:190)
                            at org.eclipse.jgit.api.SubmoduleAddCommand.call(SubmoduleAddCommand.java:185)
                    this is a known bug into jgit https://bugs.eclipse.org/bugs/show_bug.cgi?id=467611 

               - rewrite submodule url after git submodule add
                    ```
                    config.setString("remote", "origin", "url", "short:project.git");
                    config.setString("url", "https://server/repos/", "insteadOf", "short:");
                    ```
                  - Q: is the config within the origin replicated into clones ?
               - point to locally hosted repo and make it transiently unavailable 
         
         - test case registers repo init steps depending
         - per test case GitServer set up (slower as git server needs to stop/start at each test)
         - static list of pre configured repos in GitServer: Q which naming ? 
            - paas-template
            - repo-with-unreacheable-submodules
            - test-case  
   - prioritize caching:
        pull --rebase instead of clone
    
    - for paas-template support:
        - integration test: paas-template create feature-COAB-cassandra-IT:
            - simulate submodules in local volatile repo 
            - disable submodules commands unless a specific key is registered 
        - submodules without opt-in get disabled  
            # did not manage to see side effect of this config (i.e. git submodule update still triggers)
            submodule.active=false
            git config --bool submodule.active "false"
            git config --add submodule.active "expe-depls/"
            git config --add submodule.active "non-matching"
             
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

- credhub processor
    - integration test

