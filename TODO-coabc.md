   
- cassandra processor: 
    - triggers new bosh deployment: writes paas-template/paas-secret
    - tracks completion of bosh deployment: watches rendered manifest file 

- git processor support for branches
    - accepts a branch name
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
