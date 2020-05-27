## Contributing

### Releasing

Prereqs: 
* checkout the branch to release, and make sure it is up-to-date w.r.t. the github remote. 
* Make sure your IDE & git environment have the git credentials cached (as git login/password prompts might hang within intellij IDE).
 
Releasing is made using [maven release plugin](http://maven.apache.org/maven-release/maven-release-plugin/) as follows :
 
 ```shell
 
 $ mvn release:prepare --batch-mode -Dtag={your git tag} -DreleaseVersion={release version to be set} -DdevelopmentVersion={next snapshot version to be set}
 
 # ex : mvn release:prepare --batch-mode -Dtag=v0.21.0 -DreleaseVersion=0.21.0 -DdevelopmentVersion=0.22.0-SNAPSHOT
 
 ```
 
 **Hint: mind the v prefix in the tag name** to be consistent with github release naming, and URL that paas-templates deploy script expects.
 
 Circle CI build will proceed, and will trigger commands **equivalent** to a `mvn release:perform`, i.e build the jar (using `mvn install`) and upload artifacts (in our case to github, whereas maven release would upload them to a maven repository). For further details, see [release:prepare goals](http://maven.apache.org/maven-release/maven-release-plugin/prepare-mojo.html)


Following the release:
- edit the release notes in github
- clean up your local workspace using `mvn release:clean`

In case of issues, try:
* `mvn release:rollback` (which creates a new commit reverting changes)
    * possibly revert the commits in git (`git reset --hard commitid`), 
* clean up the git tag `git tag -d vXX && git push --delete origin vXX`, 
* `mvn release:clean`
* fix the root cause and retry.
   * possibly resume circle ci workflow from failed step (from the workflow page)
 
### Releasing a bug fix version

Say you have a bug in production against version 0.25.0 and need to create a bug fix version 0.25.1 without waiting for the next major(0.27.0), i.e. you need to create a 0.25.1 version out of 0.25.0

```sh
git checkout 0.25.0
git checkout -b 0.25.x
mvn release:update-versions --batch-mode -DdevelopmentVersion=0.25.1-SNAPSHOT 
git commit -am "prepare for poms 0.25.1-SNAPSHOT"
#add your fix or cherry pick it
#commit & push
mvn release:prepare --batch-mode -Dtag=0.25.1 -DreleaseVersion=0.25.1 -DdevelopmentVersion=0.25.2-SNAPSHOT
```

Then follow the same steps as for a normal release, picking up circle-ci remaining release part.


### Modification/fix to spring-cloud-open-service-broker upstream library

We use https://jitpack.io/ for building forked version of the upstream libraries and serving them as a maven repository. 

Use of the starter is however not always possible with latest milestones. So temporary inline dependencies 

However upgrading to 2.1.0.M2 seems not yet possible through the starter:
https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-open-service-broker-webmvc

https://github.com/spring-cloud/spring-cloud-open-service-broker/issues/79
https://github.com/spring-cloud/spring-cloud-open-service-broker/commit/0cf3bc0cab1914f3ade8543ad155dc108afb53d0

In case of jitpack build issues (such as repo.spring.io being down), then discard the cached failing jitpack build by connecting on https://jitpack.io/#orange-cloudfoundry/spring-cloud-open-service-broker/   authenticating with github, and deleting the failed build by clicking on the red cross, then relaunch a build, jitpack should then trigger a new build. See also https://jitpack.io/docs/BUILDING/#rebuilding

![image](https://user-images.githubusercontent.com/4748380/54263459-25d75200-4571-11e9-91b7-83a4b5e19fa4.png)

https://status.jitpack.io/
https://twitter.com/springops
https://status.bintray.com/

### Bumping springboot and spring-cloud-open-service-broker

SpringBoot version is driven by supported version in spring cloud:  
https://spring.io/projects/spring-cloud

SpringCloud Release Train 	Boot Version
Finchley SR2     Finchley        2.0.x
Greenwich M3     Greenwich       2.1.x

Finchley.SR2	"Spring Boot >=2.0.3.RELEASE and <2.0.8.BUILD-SNAPSHOT"

Therefore, the bump procedure is to bump spring-cloud-open-service-broker and inherit from its compatibility constraints (spring-cloud, spring-boot, and spring core)
 
https://github.com/spring-cloud/spring-cloud-open-service-broker/wiki/2.0-Migration-Guide


- which version of spring cloud open service broker ?
   - 2.1.0M2 remains with synchronous controller api
   - 3.0.0M2 replaces synchronous controller api with async ones, and does not seem to bring more OSB compliances
   => sticking with 2.x for now to avoid carrying reactor likely instabilities/frequent upgrades for now


#### symptoms with incompatible spring stack 

```
18:07:38.901 [main] ERROR org.springframework.boot.SpringApplication - Application run failed
java.lang.NoSuchMethodError: org.springframework.boot.builder.SpringApplicationBuilder.<init>([Ljava/lang/Object;)V
	at org.springframework.cloud.bootstrap.BootstrapApplicationListener.bootstrapServiceContext(BootstrapApplicationListener.java:161)
	at org.springframework.cloud.bootstrap.BootstrapApplicationListener.onApplicationEvent(BootstrapApplicationListener.java:102)
	at org.springframework.cloud.bootstrap.BootstrapApplicationListener.onApplicationEvent(BootstrapApplicationListener.java:68)
```

https://github.com/spring-projects/spring-boot/issues/12403
 a NoSuchMethodError very often indicates a broken setup with incompatible libraries. This is the case here as well with incompatible versions of Spring Boot and Spring Cloud. Check start.spring.io/info for more info.
