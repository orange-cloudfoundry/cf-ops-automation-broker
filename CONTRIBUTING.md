## Contributing

### Releasing

Prereqs: checkout the branch to release, and make sure it is up-to-date w.r.t. the github remote.
 
Releasing is made using [maven release plugin](http://maven.apache.org/maven-release/maven-release-plugin/) as follows :
 
 ```shell
 
 $ mvn release:prepare --batch-mode -Dtag={your git tag} -DreleaseVersion={release version to be set} -DdevelopmentVersion={next snapshot version to be set}
 
 # ex : mvn release:prepare --batch-mode -Dtag=v0.21.0 -DreleaseVersion=0.21.0 -DdevelopmentVersion=0.22.0-SNAPSHOT
 
 ```
 
 Circle CI build will proceed, and will trigger the execution of `mvn release:perform`, and upload artifacts to github. For further details, see [release:prepare goals](http://maven.apache.org/maven-release/maven-release-plugin/prepare-mojo.html)

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


### Modification/fix to upstream libraries

We use https://jitpack.io/ for building forked version of the upstream libraries and serving them as a maven repository. 
