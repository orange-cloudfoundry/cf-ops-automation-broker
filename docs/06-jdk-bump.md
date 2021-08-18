* [x] check paas-templates current jvm level:  
     * `-----> Downloading Open Jdk JRE 1.8.0_282 from https://java-buildpack.cloudfoundry.org/openjdk/bionic/x86_64/bellsoft-jre8u282%2B8-linux-amd64.tar.gz (found in cache)`
* [ ] Choose target JVM version
* [x] Check available jdk 
    * in java buildpack https://github.com/cloudfoundry/java-buildpack/releases: `OpenJDK JRE 16 	16.0.2_7` in Java Buildpack v4.41
    * in paas-templates v52 & v51: Java Buildpack v4.36 (offline): `OpenJDK JRE 15 	15.0.2_10`
* [x] check openjdk LTS https://adoptopenjdk.net/support.html: latest LTS is java 11 (Oct 2024) 
* [x] Bump paas-templates smoke tests
* [x] Bump maven build
* [x] update local IDE: File -> project structure -> project sdk -> download sdk: version 11, adopt open jdk (openJ9)
* [ ] Bump circle ci container
