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
* [ ] Fix observed incorrect behavior: after the default DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS (30 mins) the pool item get destroyed
   * set to -1 + refine logging to help debugging
   * how to test this ?
     * assert no destroy called in eager pooling unit test cases (however exec is shorter than the 30 mins duration before such eviction)
     * assert no destroy called in eager pooling integration test cases (same limitation than unit test)
   * manually validate after 30 mins the clones are not cleaned up anymore (on a long-running coab broker instance running E2E smoke tests)

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