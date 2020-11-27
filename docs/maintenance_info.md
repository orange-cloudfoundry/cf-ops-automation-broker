
Maintenance info https://github.com/orange-cloudfoundry/cf-ops-automation-broker/issues/278

## Goals

Initial step to focus on maintenance_info support (refreshing osb-client view including dashboard) with coab-vars.yml update.

Stemcell/bosh-release/deployment model upgrades are currently out of scope of COAB broker, and rather handled by coa pipelines.

This work might however be useful for plan update and params update.


Q: would this conflict with service instance upgrade pipeline ?
A: should not, since only coab-vars.yml would change

Q: is it a good idea to simplify this upgrade to not trigger coab-vars.yml change nor deployment update ?
   * would imply a mix of dashboard urls with brokered guid and backing guids

## Design of COAB and COA (models) interactions

COAB Update endpoint triggers writing of the following in the paas-templates service instance branch 
* coab-vars.yml: flat variables used in bosh deployment

The COAB model (through a COA predeploy hook) transforms `coab-vars.yml` into: 
* coab-fingerprint.yml: single fingerprint variable used to track completion of the deployment update in the resulting manifest

Then, `coab-fingerprint.yml` is inserted by deployment models into the bosh manifest as an extra `coab_completion_marker` unrecognized bosh manifest attribute (which gets ignored by bosh director). 
Being a single yaml key makes it easy to be managed by a bosh operator regardless of the key values.

COAB then waits for the presence of the `coab_completion_marker` in the bosh manifest in the secrets repo until it matches the original `coab-vars.yml` derived from request

Since getLastOperation is not fetching the paas-templates repo (as an optimization to reduce load on git server), how can we obtain the original `coab-vars.yml` to compare it against `coab_completion_marker` pushed by COA in secret repo ?
   
The `operation` field  includes the OSB provision endpoint JSON body (used to propagate the request to the nested inner broker), but this is not exactly `coab-vars.yml` content.

We propagate a finger print of `coab_completion_marker` in the operation field (used to track statefull operation in OSB API)

Other alternatives that were considered:
   - add `coab-fingerprint` key with coab-vars.yml as content to `operation` field 
      - Q: would this leak confidential information to the OSB client ? 
         - OSB client is rather trusted (OSB-CMDB)
         - coab-vars.yml is just the same data formatted differently.
      - Q: what about optimizing space to not reach max size of the OSB `state` param ?
         - coab-vars.yml is 1000 chars long without custom params 
         - comparison hash needs to be generated from normalized formatting of the two yaml structure: CoabVarsDto POJO
         - alternative hashes
            - java hashCode on CoabVarsDto
            - more complex MD5 ?
         - => **include hashCode on CoabVarsDto of the coab-vars into the state parameter and only compare the two hashcodes**
         - we keep the full coab-vars.yml into the manifest to ease troubleshooting and auditing in paas-templates git repo  


## End to end validation scenario
  
 
* [x] check catalog can be configured to return maintenance info: test noop
* [ ] design end-to-end test of service instance upgrade with noop (to simulate old service instance without `x-osb-cmdb` param)
   * [x] configure noop 49.0.0 without dashboard url 
   * [x] create one noop instance `reference-0` without x-osb-cmdb params. Expect no dashboard url
   * [x] configure noop 49.0.1 with dashboard url with v1 using backing service instance guid. 
   * [x] check  noop instance `reference-0` is upgradeable
   * [ ] create one noop instance `reference` without x-osb-cmdb params. Expect dashboard url with v1 using service instance guid
   * [ ] configure noop maintenance_info V2
   * [ ] configure noop dashboard url with v2 using brokered guid
   * [x] test that CF CC accepts `cf update-service --upgrade reference -c params.json`
       * [x] coab implements service instance update support
       * [ ] coab generates new `coab-vars.yml`
          * [x] generify existing coab-vars provisionning code to also apply on update
          * [x] adapt git processor to support pre and post update OSB life cycle hooks 
          * [x] adapt provisionning code in BoshProcessor+ BoshProcessorTest to support update
             * [x] adapt create logic to update. Always respond with update in progress for now + dashboard url
             * [x] adapt getlastoperation logic to check match of `coab_completion_marker`
             * [x] refactor to reduce duplication between create and update
       * [ ] Later, optimize response time for `noop` updates (i.e. the ones requested by developer which do not change the `coab-vars.yml` and hence don't trigger a COA build) 
          * when changes were applied to `coab-vars.yml` w.r.t.
             *  git commit & git push
             *  coa pipeline triggers, potentially deploying grafana with FQDN matching brokered guid.    
             *  manifest gets committed with `coab-fingerprint`
          * [ ] when no changes will be applied to coab-vars.yml, respond with update completed + dashboard url
             * currently will still response 202 accepted in progress, regardless of whether changes where made.    
   * [ ] assert coab-vars.yml now contains `x-osb-cmdb` param  
   * [ ] assert update completes once bosh deployment completes   
   * [ ] assert `reference` service instance now returns dashboard url with v2 using brokered guid   


   
## CF CLI behavior investigation
   
  * [x] test that CF CC accepts `cf update-service --upgrade reference -c params.json`
      * CF CLI rejects it:
      * Tested it with direct CC API: `CF_TRACE=true cf curl -X PUT /v2/service_instances/39308492-68d3-4601-adb6-8f76f37392f5?accepts_incomplete=true -d @/tmp/maintenance_info.json`
```json
{
  "maintenance_info": {
    "description": "Dashboard url with backing service guids",
    "version": "49.0.1"
  },
  "parameters": {
          "aparam":"avalue"
  }
}
```

properly received by broker

```
17:58:14.530: [APP/PROC/WEB.0] 2020-11-04 16:58:14.529  INFO 27 --- [nio-8080-exec-5] o.s.c.s.c.ServiceInstanceController      : Updating service instance
17:58:14.530: [APP/PROC/WEB.0] 2020-11-04 16:58:14.529 DEBUG 27 --- [nio-8080-exec-5] o.s.c.s.c.ServiceInstanceController      : request=ServiceBrokerRequest{platformInstanceId='null', apiInfoLocation='api.redacted-domain.org/v2/info', originatingIdentity=Context{platform='cloudfoundry', properties={user_id=321ae0c8-1289-4e49-9aa4-4fca806754f1}}', requestIdentity=09b20f4a-6852-4951-8f3a-c8bcbe27d1fa}AsyncServiceBrokerRequest{asyncAccepted=true}AsyncParameterizedServiceInstanceRequest{parameters={aparam=avalue}, context=Context{platform='cloudfoundry', properties={spaceGuid=d8d14da7-7ac8-4a6b-b17b-8544c28e514a, spaceName=coa-noop-smoke-tests, organizationName=service-sandbox, instanceName=dummy-name, organizationGuid=b65a1232-add9-49ab-8bf1-283ddc08c0de}}}UpdateServiceInstanceRequest{serviceDefinitionId='noop-ondemand-service', planId='noop-ondemand-plan', previousValues=PreviousValues{planId='noop-ondemand-plan'maintenanceInfo='MaintenanceInfo{version='49.0.1, description='null}'}, serviceInstanceId='39308492-68d3-4601-adb6-8f76f37392f5', maintenanceInfo='MaintenanceInfo{version='49.0.1, description='null}'}
17:58:14.530: [APP/PROC/WEB.0] 2020-11-04 16:58:14.529  INFO 27 --- [nio-8080-exec-5] o.s.c.s.c.ServiceInstanceController      : Updating service instance succeeded
17:58:14.530: [APP/PROC/WEB.0] 2020-11-04 16:58:14.529 DEBUG 27 --- [nio-8080-exec-5] o.s.c.s.c.ServiceInstanceController      : serviceInstanceId=39308492-68d3-4601-adb6-8f76f37392f5, response=AsyncServiceInstanceResponse{async=false, operation='null'}UpdateServiceInstanceResponse{dashboardUrl='null'}
17:58:14.533: [RTR.1] coa-noop-broker.redacted-domain.org - [2020-11-04T16:58:14.430017956Z] "PATCH /v2/service_instances/39308492-68d3-4601-adb6-8f76f37392f5?accepts_incomplete=true HTTP/1.1" 200 754 2 "-" "HTTPClient/1.0 (2.8.3, ruby 2.5.5 (2019-03-15))" "192.168.35.66:38488" "192.168.35.79:61090" x_forwarded_for:"192.168.35.66" x_forwarded_proto:"https" vcap_request_id:"effa42ae-d451-4d89-67e8-2cc902aa8319" response_time:0.102658 gorouter_time:0.000581 app_id:"06444d72-af92-4539-95c1-3dad397f724c" app_index:"0" x_cf_routererror:"-" x_b3_traceid:"822be555cf26c586" x_b3_spanid:"822be555cf26c586" x_b3_parentspanid:"-" b3:"822be555cf26c586-822be555cf26c586"
```

## Implementation tasks
  
  
* [x] check bump of sc-osb. Coab is running 2.1.2.RELEASE. 
   * Maintenance_info support requires 3.1.2.RELEASE or 3.2.0-M1
   * Bump to 3.1.x or 3.2.x may imply boot and spring bumps (see [version-compatibility](https://github.com/spring-cloud/spring-cloud-open-service-broker#version-compatibility))])
   
Spring Cloud Open Service Broker | Open Service Broker API | Spring Boot | Spring Framework
-- | -- | -- | --
3.2.x | 2.15 | 2.3.x | 5.2.x
3.1.x | 2.15 | 2.2.x | 5.2.x
3.0.x | 2.14 | 2.1.x | 5.1.x
2.1.x | 2.14 | 2.0.x | 5.0.x
   * Currently COA is running:
      * SpringCloud dependencies Hoxton.SR8. Hoxton.SR8 is compatible with Spring Boot 2.3.x and 2.2.x. See https://spring.io/blog/2020/08/28/spring-cloud-hoxton-sr8-has-been-released
      * SpringBoot 2.3.3
      * Spring 5.2.8.RELEASE
   * SC-OSB bump requires using reactive apis, but we remain with servlet blocking stack. See https://github.com/spring-cloud/spring-cloud-open-service-broker/commit/121291aeec0f565bc0202d5c1d85c2eb27becbea    
   
   
* [x] check coab-noop proto with fingerprint
   * [x] find a way to assert this into the deployment models
   * [x] refine coab noop model with realistic coab-vars.yml (including -- doc separator)
      * [ ] test that the prepare script also works if --- doc separator is missing ??
   * [ ] verify coab-noop smoke test is still green with coab-vars.yml generated by coab
      * [ ] need to rebase noop service instance branch against feature-coab-v49-3 + reset
   * [ ] apply to all other models
* [x] inject completion marker hashcode  into last operation state
   * [x] inject coabvars to PipelineCompletionTracker.getPipelineOperationStateAsJson()
      * coabvars missing in the case of delete
         * (delete is optionally skipped and not forwarded to inner broker)
         * provide null coab-vars reference for delete: won't be checked for completion anyway
   * [x] deserialize coab-vars from deployment manifest
      * Pb: coab-vars contains arbitrary nested content (from params) and so does not suit plain deserialization using Gson unless type
      * Use Jackson instead of Gson
      * [x] Add equals/hashcode/toString to CoabVarsDto
      * [x] Add parsing code into VarsFilesYmlFormatter
         * [x] consider renaming to reflect also parsing
      * [x] Add test coverage for osb-cmdb sample inputs in VarsFilesYmlFormatterTest
      * [x] add parsing to SecretsGenerator contract
         * [x] Introduce New DTO to parse the generated manifest, only picking up CoabVarsDTO: BoshDeploymentManifestDTO
            * [x] Add unit test for parsing sample manifests 
         * [x] Add test coverage
         * [x] Consider refactoring into SecretsManager to surface parsing role: 
            * Keeping it as SecretsGenerator for now because of :
               * Consistency with TemplatesGenerator
               * Avoid too many cosmetic changes that would slow down contribs by JCL38  
      * [x] adapt PipelineCompletionTracker to deserialize using SecretsGenerator
         * [x] Add test coverage 
* [x] Refine BoshServiceProvisionningTest
   * [x] Adapt generated manifest to include completion marker
      * Need to capture CoabVarsDto or parse it from disk to include it as a completion marker in the manifest (Possibly through BoshDeploymentManifestDTO)
         * [x] Extract CoabVarsDtoBuilder from BoshProcessor
   * [x] Add test of plan upgrade
      * Fix PipelineCompletionTracker to handle UpdateServiceInstanceRequest 
         * Q: do we need to propagate OSB update call to inner broker ? Not yet, see details below.
            * cf-mysql: not yet
            * mongo-db: no
            * redis: no
            * k3s 
   * [ ] Add test of maintenance_info upgrade
      * Pb: in 1st naive test impl, coab-vars.yml does not yet contain maintenance_info, so the `cf update-service --upgrade` push no changes and this does not trigger a COA build
         * CoabVarsDto.previousValue may contain previous maintenance_info, and could be sufficient to trigger a build 
         * `maintenance_info` is however useful for 
            * auditing purposes
            * in the future, comply with osb 1.17 which returns `maintenance_info` in service instance fetch endpoint 
      * [x] Store `maintenance_info` in coab-vars.yml
         * [x] Add new `maintenance_info` field in CoabVarsDto
         * [x] Refine CoabVarsDtoBuilder to set it
      * [x] refine test request to include maintenance_info 
   * [ ] Refactor OsbBuilderHelper
      * rename update into upgrade
      * use constants for plan, service guid   
      
### Plan upgrade validator impl

* [x] Define properties
* [x] Define processor and its message formatter
* [x] Register processor in application
* [x] Overall test in BoshServiceProvisionningTest
* [x] Update reference properties
* [ ] Update doc 