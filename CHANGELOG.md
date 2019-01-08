
## 1.18.0 (unreleased)

### IMPROVEMENTS

- Part of Osb client are contributed upstream in  

### BUG FIXES
 
### CONFIGURATION CHANGES

#### `CATALOG_YML` environment variable is deprecated.

Use `spring.cloud.openservicebroker.catalog` environment variable to set catalog config in a YAML format. See [spring-cloud-open-service-broker] for a sample YML and raw properties configuration, such as [catalog-minimal.yml](https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/2.x/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-minimal.yml) [catalog-full.properties](https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/2.x/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-full.properties)


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

becomes:

```yaml
CATALOG_YML: |
      spring:
        cloud:
          openservicebroker:
        catalog:
          services:
          - id: example-service
            name: example
            description: A simple example
            bindable: true
            tags:
            - example
            - tags
            plans:
            - id: simple-plan
              name: standard
              description: A simple plan
```

#### Springboot auth properties changed

`security.user.name` and `security.user.password` * now become `spring.security.user.name` and `spring.security.user.password`

See https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Configuration-Changelog
 
