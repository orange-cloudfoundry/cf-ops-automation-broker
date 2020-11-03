
Maintenance info https://github.com/orange-cloudfoundry/cf-ops-automation-broker/issues/278

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
 
* [ ] check catalog can be configured to return maintenance info: test noop
* [ ] design end-to-end test of service instance upgrade with noop (to simulate old service instance without x-osb-cmdb)
   * [ ] configure noop dashboard url with v1 using service instance guid
   * [ ] create one noop instance `reference` without x-osb-cmdb params. Expect dashboard url with v1 using service instance guid
   * [ ] configure noop maintenance_info V2
   * [ ] configure noop dashboard url with v2 using brokered guid
   * [ ] test that CF accepts `cf update-service --upgrade reference -c params.json`
       * [ ] coab generates new coab-vars.yml in RAM and compares it with disk version
       * [ ] if changes will be applied to coab-vars.yml, responds with update in progress until manifest gets updated + dashboard url
          * [ ] coab-vars.yml gets generated with incremented `epoq:<epoq>.<timestamp>`
          * [ ] attempt to git commit & git push
          * [ ] coa pipeline triggers, potentially deploying grafana with FQDN matching brokered guid.    
          * [ ] manifest gets committed with `epoq:<epoq>.<timestamp>`
       * [ ] if no changes will be applied to coab-vars.yml, responds with update completed + dashboard url    
   * [ ] assert coab-vars.yml now contains x-osb-cmdb   
   * [ ] assert update completes once bosh deployment completes   
   * [ ] assert `reference` service instance now returns dashboard url with v2 using brokered guid   
* [ ] Implement OSB service instance update which synchronously
* [ ] Store maintenance_info in coab-vars.yml

Q: would this conflict with service instance upgrade pipeline ?
A: should not, since only coab-vars.yml would change

Q: is it a good idea to simplify this upgrade to not trigger coab-vars.yml change nor deployment update ?
   * would imply a mix of dashboard urls with brokered guid and backing guids