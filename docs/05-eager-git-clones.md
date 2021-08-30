Support for https://github.com/orange-cloudfoundry/cf-ops-automation-broker/issues/421

## Overview

* [x] double read apache pooling doc related to min idle to understand at what time the min idle pool is refilled when consumming one: would it be synchronously at 1st take() request ? No it will be async in the idle eviction thread
   * See https://github.com/apache/commons-pool/blob/48c289d95c2374ee11e3276a8bcb93b7f99015be/src/main/java/org/apache/commons/pool2/impl/GenericKeyedObjectPool.java#L1130-L1146
     > getMinIdlePerKey():
     > Gets the target for the minimum number of idle objects to maintain in
     > each of the keyed sub-pools. This setting only has an effect if it is
     > positive and {@link #getTimeBetweenEvictionRunsMillis()} is greater than
     > zero. If this is the case, an attempt is made to ensure that each
     > sub-pool has the required minimum number of instances during idle object
     > eviction runs.
   * [x] refine test to assert this behavior 
      * [x] synchronous behavior
      * [ ] async behavior sleeping for eviction thread to run
* [x] Configure [GenericKeyedObjectPool](https://github.com/apache/commons-pool/blob/48c289d95c2374ee11e3276a8bcb93b7f99015be/src/main/java/org/apache/commons/pool2/impl/GenericKeyedObjectPool.java#L89) with non zero configureable `MinIdlePerKey` property
* [x] Initialize keys representing paas-templates and paas-secret Context keys in [preparePool](https://github.com/apache/commons-pool/blob/48c289d95c2374ee11e3276a8bcb93b7f99015be/src/main/java/org/apache/commons/pool2/impl/GenericKeyedObjectPool.java#L1461-L1475)
    * [x] Possibly extract https://github.com/orange-cloudfoundry/cf-ops-automation-broker/blob/97b73ef7703bdfd941d771f73edb54ed113489ef/cf-ops-automation-bosh-broker/src/main/java/com/orange/oss/cloudfoundry/broker/opsautomation/ondemandbroker/sample/BoshBrokerApplication.java#L349-L352
    * [ ] move out of constructor in spring bean initializer method ?
* [x] Define the right default for the `getTimeBetweenEvictionRunsMillis()` for our use cases
  * Our goal is that incoming request 95 percentile don't perform sync clones, but rather reuse an idle pooled clone
  * Time to fetch a clone time might be up to 1 min elapsed
  * Concurrent OSB call rate
     * Smoke tests:
       * batch delete of old service instances: sync deprovisioning + async delete polling.
       * single sync provisionning + async polling
     * End users: single sync provisionning + async polling
  * Time between successive OSB calls:
     * CF CC broker polling period: 60s
        * Per platform default https://github.com/cloudfoundry/capi-release/blob/863667d3745b45ec04c14f6b8a80bb0eec5e2ec2/jobs/cloud_controller_worker/spec#L472-L478
          >  cc.broker_client_default_async_poll_interval_seconds:
          >    default: 60
          >    description: "Specifies interval on which the CC will poll a service broker for asynchronous actions"
          >
          >  cc.broker_client_max_async_poll_duration_minutes:
          >    default: 10080
          >    description: "The max duration the CC will fetch service instance state from a service broker. Default is 1 week"       
        * Configureable per service plan max period: https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#polling-interval-and-duration
  * impact if period too small (0,5s) and min-idle=1 
      * (an incoming osb call would systematically trigger a single async git clone to gitlab (like today without eager pooling))
      * systematic concurrency between OSB triggered git operations (push/fetch) and async pool refill (clone + reset)
      * would evict too many clones too early (max default is 8)
  * impact if period is too large (5 mins): idle pool exhausted and OSB calls get timeout
  * Decision to set to 1s
* [x] Support configuration of pooling. 
   * Per repo: templates/secrets, in a distinct PoolingProperties class to avoid too large GitProperties class 
* [x] Update/fix tests which check that there is no git clone leaks
* [x] Update documentation
* [x] Fix observed incorrect behavior: after the default DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS (30 mins) the pool item get destroyed
   * set to -1 + refine logging to help debugging
   * how to test this ?
     * assert no destroy called in eager pooling unit test cases (however exec is shorter than the 30 mins duration before such eviction)
     * assert no destroy called in eager pooling integration test cases (same limitation than unit test)
   * manually validate after 30 mins the clones are not cleaned up anymore (on a long-running coab broker instance running E2E smoke tests)
* [x] Try to reduce risk/impact of a slow start due to slow repo prefetching (observed once at 60s, would fail at 180s)
   * [ ] Try to configure pool to perform prefetching in evictor thread
      * [X] By configuring minIdle=0 before calling `preparePool`: the key pool isn't added
      * [ ] By scheduling the pool prefetching configuration after start up on another thread
   * [x] Better assess the impact of a slow start: how long would diego try to restart the app ? 200 
      * [x] Inject a 180s sleep loop at start time and observe the up time with the curl
         * Deploy manually with a cf push with --strategy rolling (deploying through ci requires passing build, whereas tests would be red by injected sleep)
         * Watch the up time with the curl
         * => confirmed rolling update is properly working without downtime 
      * [x] Read the docs
         * https://docs.cloudfoundry.org/devguide/deploy-apps/healthchecks.html 

> When an app instance crashes, Diego immediately attempts to restart the app instance several times. After three failed restarts, 
> Cloud Foundry waits 30 seconds before attempting another restart. The wait time doubles each restart until the ninth restart, 
> and remains at that duration until the 200th restart. After the 200th restart, Cloud Foundry stops trying to restart the app instance
   * [x] deep dive on zdt and why we observed it non effective on orange labs fe-int environment on coab-noop
      * [x] verify zdt is enabled for coab in generated pipelines
      * [x] read the docs
      * [x] double check coab-noops traces
```
Coab-noop is overiding default CF_PUSH_OPTIONS=--strategy rolling with --strategy=null as this option is not supported by CF cli with multiple-apps manifests

CF push options: 
```     


## Details

### eager pooling at start up breaks BoshServiceProvisionningTest as git server not yet started

BoshServiceProvisionningTest uses SpringBootTest but prereq git server is started in   `@BeforeEach startGitServer()` and `@AfterEach stopGitServer()`

GitServer initialization relies on DeploymentProperties deploymentProperties to be available. This is currently loaded/injected in the SpringContext

Options:
* [ ] transiently turn off eager pooling in BoshServiceProvisionningTest to get smoke tests feedback
* [x] convert the git initialization/cleanup as spring initializer/deinitializers/configuration
     * https://stackoverflow.com/questions/63712543/beforeall-junit-spring-boot-test-alternative-that-runs-when-application-context
     * As a distinct @Configuration still runs too late, after failure of the Bean instanciation
       * [x] move to bean initializer, in hope that the GitServer will be instanciated before the bean initialization: not better
       * [x] use @Order highest precedence. **Not clear why this did not work**
       * [ ] move eager pooling to spring application start time using a spring listener
       * this stackoverflow suggest this is hard https://stackoverflow.com/a/55007709/1484823
     * [ ] As an ApplicationEventListener before spring beans get initialized https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.spring-application.application-events-and-listeners 
* [x] Move test git server initialization to @BeforeAll
    * Pb: beforeClass implies static method but configuration of git content depends on spring-loaded properties file
* [x] **Move git init to junit test initializer** to initialize before spring context loads
  * See https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-testing-annotation-testexecutionlisteners
  * Hinted by https://github.com/spring-projects/spring-boot/issues/20697
  * [x] rework git repo initialization to not rely on DeploymentProperties anymore, and convert to @BeforeAll
      * currently only uses modelDeployment=mongodb (instead of `cassandravarsops` default)
          * load a Properties object
          * find a way to load the DeploymentProperties using springboot
          * [x] **provide a default DeploymentProperties with hardcoded value**
      * Still same ordering problem: by design according to https://stackoverflow.com/a/46981615/1484823
  

org.junit.platform.commons.JUnitException: @BeforeAll method 'public void com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample.BoshServiceProvisionningTest.startGitServer() throws java.io.IOException' must be static unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS).

```
Factory method 'poolingSecretsGitManager' threw exception; nested exception is java.lang.RuntimeException: java.lang.IllegalArgumentException: org.eclipse.jgit.api.errors.TransportException: git://127.0.0.1:9418/paas-secrets.git: Connection refused (Connection refused)
Caused by: java.lang.RuntimeException: java.lang.IllegalArgumentException: org.eclipse.jgit.api.errors.TransportException: git://127.0.0.1:9418/paas-secrets.git: Connection refused (Connection refused)
Caused by: java.lang.IllegalArgumentException: org.eclipse.jgit.api.errors.TransportException: git://127.0.0.1:9418/paas-secrets.git: Connection refused (Connection refused)
Caused by: org.eclipse.jgit.api.errors.TransportException: git://127.0.0.1:9418/paas-secrets.git: Connection refused (Connection refused)
Caused by: org.eclipse.jgit.errors.TransportException: git://127.0.0.1:9418/paas-secrets.git: Connection refused (Connection refused) 
```

### classpath conflicts apache commons pool

classpath conflict on commons-pool with spring-boot
`<commons-pool2.version>2.9.0</commons-pool2.version>`
https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html
org.apache.commons: commons-pool2: 2.9.0

```
***************************
APPLICATION FAILED TO START
***************************

Description:

An attempt was made to call a method that does not exist. The attempt was made from the following location:

    com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.PooledGitManager.<init>(PooledGitManager.java:43)

The following method did not exist:

    org.apache.commons.pool2.impl.GenericKeyedObjectPool.setTimeBetweenEvictionRuns(Ljava/time/Duration;)V

The method's class, org.apache.commons.pool2.impl.GenericKeyedObjectPool, is available from the following locations:

    jar:file:/home/circleci/.m2/repository/org/apache/commons/commons-pool2/2.9.0/commons-pool2-2.9.0.jar!/org/apache/commons/pool2/impl/GenericKeyedObjectPool.class

The class hierarchy was loaded from the following locations:

    org.apache.commons.pool2.impl.GenericKeyedObjectPool: file:/home/circleci/.m2/repository/org/apache/commons/commons-pool2/2.9.0/commons-pool2-2.9.0.jar
    org.apache.commons.pool2.impl.BaseGenericObjectPool: file:/home/circleci/.m2/repository/org/apache/commons/commons-pool2/2.9.0/commons-pool2-2.9.0.jar
    org.apache.commons.pool2.BaseObject: file:/home/circleci/.m2/repository/org/apache/commons/commons-pool2/2.9.0/commons-pool2-2.9.0.jar 
```


### Failed start using timeout

```
$ cf push [...]
Instances starting...
Instances starting...
Instances starting...
Start app timeout

TIP: Application must be listening on the right port. Instead of hard coding the port, use the $PORT environment variable.

Use 'cf logs coa-noop-broker --recent' for more information
FAILED
```


```
2021-08-30T11:53:34.13+0200 [HEALTH/0] ERR Failed to make TCP connection to port 8080: connection refused
2021-08-30T11:53:34.13+0200 [CELL/0] ERR Failed after 3m0.278s: readiness health check never passed.
2021-08-30T11:53:34.13+0200 [CELL/SSHD/0] OUT Exit status 0
2021-08-30T11:53:49.56+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 stopping instance 2c54444f-b6f9-4a8b-7c9b-9748
2021-08-30T11:53:49.56+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 destroying container for instance 2c54444f-b6f9-4a8b-7c9b-9748
2021-08-30T11:53:49.59+0200 [CELL/0] OUT Cell a56f78db-fac0-4232-a0f8-e85289f07b8e creating container for instance bc4caf2c-4238-482d-7279-e25b
2021-08-30T11:53:49.80+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully destroyed container for instance 2c54444f-b6f9-4a8b-7c9b-9748
2021-08-30T11:53:49.90+0200 [CELL/0] OUT Cell a56f78db-fac0-4232-a0f8-e85289f07b8e successfully created container for instance bc4caf2c-4238-482d-7279-e25b
2021-08-30T11:53:50.00+0200 [CELL/0] OUT Downloading droplet...
2021-08-30T11:53:53.64+0200 [CELL/0] OUT Downloaded droplet (89M)
2021-08-30T11:53:53.64+0200 [CELL/0] OUT Starting health monitoring of container
2021-08-30T11:56:54.07+0200 [HEALTH/0] ERR Failed to make TCP connection to port 8080: connection refused
2021-08-30T11:56:54.07+0200 [CELL/0] ERR Failed after 3m0.428s: readiness health check never passed.
2021-08-30T11:56:54.07+0200 [CELL/SSHD/0] OUT Exit status 0
2021-08-30T11:57:09.53+0200 [CELL/0] OUT Cell a56f78db-fac0-4232-a0f8-e85289f07b8e stopping instance bc4caf2c-4238-482d-7279-e25b
2021-08-30T11:57:09.53+0200 [CELL/0] OUT Cell a56f78db-fac0-4232-a0f8-e85289f07b8e destroying container for instance bc4caf2c-4238-482d-7279-e25b
2021-08-30T11:57:09.57+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 creating container for instance 506135e0-027d-4df0-7115-2e6e
2021-08-30T11:57:09.81+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully created container for instance 506135e0-027d-4df0-7115-2e6e
2021-08-30T11:57:09.85+0200 [CELL/0] OUT Cell a56f78db-fac0-4232-a0f8-e85289f07b8e successfully destroyed container for instance bc4caf2c-4238-482d-7279-e25b
2021-08-30T11:57:10.03+0200 [CELL/0] OUT Downloading droplet...
2021-08-30T11:57:10.32+0200 [CELL/0] OUT Downloaded droplet
2021-08-30T11:57:10.32+0200 [CELL/0] OUT Starting health monitoring of container
2021-08-30T12:00:10.50+0200 [HEALTH/0] ERR Failed to make TCP connection to port 8080: connection refused
2021-08-30T12:00:10.50+0200 [CELL/0] ERR Failed after 3m0.179s: readiness health check never passed.
2021-08-30T12:00:10.51+0200 [CELL/SSHD/0] OUT Exit status 0
2021-08-30T12:00:26.01+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 stopping instance 506135e0-027d-4df0-7115-2e6e
2021-08-30T12:00:26.01+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 destroying container for instance 506135e0-027d-4df0-7115-2e6e
2021-08-30T12:00:26.26+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully destroyed container for instance 506135e0-027d-4df0-7115-2e6e
2021-08-30T12:00:56.53+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 creating container for instance b1294f29-eef6-4417-4971-3cc9
2021-08-30T12:00:56.78+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully created container for instance b1294f29-eef6-4417-4971-3cc9
2021-08-30T12:00:57.17+0200 [CELL/0] OUT Downloading droplet...
2021-08-30T12:00:57.45+0200 [CELL/0] OUT Downloaded droplet
2021-08-30T12:00:57.45+0200 [CELL/0] OUT Starting health monitoring of container
2021-08-30T12:03:57.64+0200 [HEALTH/0] ERR Failed to make TCP connection to port 8080: connection refused
2021-08-30T12:03:57.64+0200 [CELL/0] ERR Failed after 3m0.182s: readiness health check never passed.
2021-08-30T12:03:57.64+0200 [CELL/SSHD/0] OUT Exit status 0
2021-08-30T12:04:13.05+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 stopping instance b1294f29-eef6-4417-4971-3cc9
2021-08-30T12:04:13.05+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 destroying container for instance b1294f29-eef6-4417-4971-3cc9
2021-08-30T12:04:13.31+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully destroyed container for instance b1294f29-eef6-4417-4971-3cc9
2021-08-30T12:05:26.81+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 creating container for instance b4f69f42-9b7a-4f9f-6e03-6538
2021-08-30T12:05:27.06+0200 [CELL/0] OUT Cell 1a24fee7-f041-4109-a287-2a86954735e9 successfully created container for instance b4f69f42-9b7a-4f9f-6e03-6538
2021-08-30T12:05:27.30+0200 [CELL/0] OUT Downloading droplet...
2021-08-30T12:05:27.59+0200 [CELL/0] OUT Downloaded droplet
2021-08-30T12:05:27.59+0200 [CELL/0] OUT Starting health monitoring of container
```