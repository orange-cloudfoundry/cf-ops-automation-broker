- validate paas-template and paas-secret specs: 2 cassandra deployments: 3 creations, 1 deletions
    - validate vars/ops files for
       - route registrar
       - deployment name
    - validate symlinks in paas-template
    
- cassandra processor: 
    - triggers new bosh deployment: writes paas-template/paas-secret
    - tracks completion of bosh deployment: watches rendered manifest file 

- git processor support for branches

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
