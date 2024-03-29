## 1.19.0 (unreleased)

## 1.18.0 

### IMPROVEMENTS

- Part of Osb client are contributed upstream in https://github.com/spring-cloud/spring-cloud-open-service-broker 

### BUG FIXES
 
* Metadata Plan not returned to cf #49 
* Optimize size of state object returned to OSB API  #45 
* Add retries when git push fails # 44
* Set up git clone caches #42 
* springboot & OSB bump (security + OSB 2.14 support) #39 
 
### CONFIGURATION CHANGES

#### Springboot auth properties changed

`security.user.name` and `security.user.password` * now become `spring.security.user.name` and `spring.security.user.password`

See https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Configuration-Changelog
 

#### Optional `CATALOG_YML` environment variable content change.

The `CATALOG_YML` environment variable content has changed (as this is now fully leveraging [spring cloud open service broker (SCOSB) library support](https://github.com/spring-cloud/spring-cloud-open-service-broker), and the yaml content
now needs to start at `spring.cloud.openservicebroker`. 

The previous format 

```yaml
CATALOG_YML: |
  servicebroker:
    catalog:
      services:
      - id: cassandra-ondemand-service
        name: cassandra-ondemand
        description: "On demand cassandra dedicated cluster"
        bindable: true
        plans:
          - id: cassandra-ondemand-plan
            name: default
            description: Default plan (beta).
        tags:
          - cassandra
          - document
        metadata:
          displayName: ondemand
          imageUrl: http://cassandra.apache.org/img/cassandra_logo.png
          longDescription: "A dedicated on-demand cassandra cluster with a single keyspace. This is beta: Not yet monitored and backed up. Default sizing can't yet be changed at creation or update."
          providerDisplayName: Orange
          documentationUrl: https://github.com/orange-cloudfoundry/cassandra-cf-service-boshrelease
          supportUrl: https://github.com/orange-cloudfoundry/cassandra-cf-service-boshrelease
```

can optionally be specified as following native SCOSB format. However, previous format is still supported.

```yaml
CATALOG_YML: |
  spring:
    cloud:
      openservicebroker:
        catalog:
          services:
          - id: cassandra-ondemand-service
            name: cassandra-ondemand
            description: "On demand cassandra dedicated cluster"
            bindable: true
            plans:
              - id: cassandra-ondemand-plan
                name: default
                description: Default plan (beta).
            tags:
              - cassandra
              - document
            metadata:
              displayName: ondemand
              imageUrl: http://cassandra.apache.org/img/cassandra_logo.png
              longDescription: "A dedicated on-demand cassandra cluster with a single keyspace. This is beta: Not yet monitored and backed up. Default sizing can't yet be changed at creation or update."
              providerDisplayName: Orange
              documentationUrl: https://github.com/orange-cloudfoundry/cassandra-cf-service-boshrelease
              supportUrl: https://github.com/orange-cloudfoundry/cassandra-cf-service-boshrelease
```

The reference of full syntax is available in the library at [catalog-full.yml](https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/2.x/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-full.yml) 

Note that an alternative to use of Use `CATALOG_YML` environment variable, is to set flat properties as illustrated into [catalog-full.properties](https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/2.x/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-full.properties) and [spring-cloud-open-service-broker manual](https://docs.spring.io/spring-cloud-open-service-broker/docs/current/reference/html5/#_providing_a_catalog_using_properties)) 



