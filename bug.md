Solutions:

* live with it for now: ignore polluting red pipeline (and future red alerts)
* COAB performs async delete
    * step 1: deletes enable-deployment.yml: triggers update-pipeline & removal of the deployment
    * Step 2: finish paas-secret clean up (remaining files)
* modify delete support in COA 
    * enable-deployment.yml supports a new flag 
        * style: declarative vs imperative
            * declarative
                * desired_state=enabled|disabled
                * expires_at=date
            * imperative
                * action=delete
                * action=recover
        * + optional async paas-secret clean up
            * in delete pipeline
            * in concourse execute deploy.sh (in each deployment)
        * impacts of async COA paas-secret clean up:
            * mixed ownership of files in paas-secret
                * currently service_instances owned by paas-secret
        * impact of lack of clean up of paas-secret files
            * possible colision on service-id recycling by CC API
            * cognitive load on ops team
            
Possible related stories
* undelete
* dev deployment expiration/leases
* usage based billing (emited by the deployment).
