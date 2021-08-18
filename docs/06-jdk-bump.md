* [x] check paas-templates current jvm level:  
     * `-----> Downloading Open Jdk JRE 1.8.0_282 from https://java-buildpack.cloudfoundry.org/openjdk/bionic/x86_64/bellsoft-jre8u282%2B8-linux-amd64.tar.gz (found in cache)`
* [ ] Choose target JVM version: latest LTS available available in production
  * [x] Check available jdk 
      * in java buildpack https://github.com/cloudfoundry/java-buildpack/releases: `OpenJDK JRE 16 	16.0.2_7` in Java Buildpack v4.41
        * in paas-templates v52 & v51: Java Buildpack v4.36 (offline): `OpenJDK JRE 15 	15.0.2_10`
  * [x] check openjdk LTS https://adoptopenjdk.net/support.html: latest LTS is java 11 (Oct 2024) 
* [x] Bump paas-templates smoke tests
* [x] Bump maven build
* [ ] update local IDE: 
   * [x] File -> project structure -> project sdk -> download sdk: version 11, adopt open jdk (openJ9)
   * [x] File -> project structure -> project sdk -> project language level: 11
   * [ ] fix IDE compilation error: tried without luck: 
     * [x] restart ide, 
     * [x] maven reimport, 
     * [x] rebuild, 
     * [x] invalidate ide caches 
     * [x] bump maven modules
     * [ ] recreate project from scratch in intellij
     * Not much more from https://stackoverflow.com/questions/54137286/error-java-invalid-target-release-11-intellij-idea
     

```
Executing pre-compile tasks...
Running 'before' tasks
Checking sources
Copying resources... [spring-boot-starter-servicebroker-catalog]
Copying resources... [cf-ops-automation-broker-framework]
Copying resources... [cf-ops-automation-broker-core]
Parsing java... [cf-ops-automation-broker-framework]
java: warning: source release 11 requires target release 11
Checking dependencies... [cf-ops-automation-broker-framework]
Dependency analysis found 0 affected files
Errors occurred while compiling module 'cf-ops-automation-broker-framework'
javac 11 was used to compile java sources
Finished, saving caches...
Compilation failed: errors: 1; warnings: 0
Executing post-compile tasks...
Synchronizing output directories...
18/08/2021 13:40 - Build completed with 1 error and 0 warnings in 46 sec, 334 ms 
```  

* [x] Bump circle ci container
* [ ] fix failed tests

```
loads_yml_env_var_as_property_source_into_spring_context_and_convert_to_scosb_format - com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializerTest

java.lang.ClassCastException: class org.springframework.boot.origin.OriginTrackedValue$OriginTrackedCharSequence cannot be cast to class java.lang.String (org.springframework.boot.origin.OriginTrackedValue$OriginTrackedCharSequence is in unnamed module of loader 'app'; java.lang.String is in module java.base of loader 'bootstrap')
	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializerTest.loads_yml_env_var_as_property_source_into_spring_context_and_convert_to_scosb_format(YamlCataloglAsEnvironmentVarApplicationContextInitializerTest.java:78)
 
```

```
loads_yml_env_vars_as_catalog_bean - com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializerTest

java.lang.AssertionError: 

Expecting:
 <Unstarted application context org.springframework.boot.test.context.assertj.AssertableApplicationContext[startupFailure=java.lang.ClassCastException]>
to have a single bean of type:
 <org.springframework.cloud.servicebroker.model.catalog.Catalog>:
but context failed to start:
 java.lang.ClassCastException: class org.springframework.boot.origin.OriginTrackedValue$OriginTrackedCharSequence cannot be cast to class java.lang.String (org.springframework.boot.origin.OriginTrackedValue$OriginTrackedCharSequence is in unnamed module of loader 'app'; java.lang.String is in module java.base of loader 'bootstrap')
 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializer.convertPropertySourceToScOsbKeyPrefix(YamlCataloglAsEnvironmentVarApplicationContextInitializer.java:60)
 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializer.initialize(YamlCataloglAsEnvironmentVarApplicationContextInitializer.java:44)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.lambda$configureContext$3(AbstractApplicationContextRunner.java:446)
 	at java.base/java.util.ArrayList.forEach(ArrayList.java:1541)
 	at java.base/java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1085)
 	at java.base/java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1085)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.configureContext(AbstractApplicationContextRunner.java:446)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.createAndLoadContext(AbstractApplicationContextRunner.java:423)
 	at org.springframework.boot.test.context.assertj.AssertProviderApplicationContextInvocationHandler.getContextOrStartupFailure(AssertProviderApplicationContextInvocationHandler.java:61)
 	at org.springframework.boot.test.context.assertj.AssertProviderApplicationContextInvocationHandler.<init>(AssertProviderApplicationContextInvocationHandler.java:48)
 	at org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider.get(ApplicationContextAssertProvider.java:112)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.createAssertableContext(AbstractApplicationContextRunner.java:412)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.lambda$null$0(AbstractApplicationContextRunner.java:382)
 	at org.springframework.boot.test.util.TestPropertyValues.applyToSystemProperties(TestPropertyValues.java:175)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.lambda$run$1(AbstractApplicationContextRunner.java:381)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.withContextClassLoader(AbstractApplicationContextRunner.java:392)
 	at org.springframework.boot.test.context.runner.AbstractApplicationContextRunner.run(AbstractApplicationContextRunner.java:381)
 	at com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.YamlCataloglAsEnvironmentVarApplicationContextInitializerTest.loads_yml_env_vars_as_catalog_bean(YamlCataloglAsEnvironmentVarApplicationContextInitializerTest.java:169)
 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
```
   * [ ] Try running debugger as suggested in https://github.com/spring-projects/spring-boot/issues/8540#issuecomment-285409843
   * [ ] Try https://dzone.com/articles/migrating-springboot-applications-to-latest-java-v
