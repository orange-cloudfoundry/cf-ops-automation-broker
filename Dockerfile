FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD https://github.com/orange-cloudfoundry/cf-ops-automation-broker/releases/download/v0.27.0/cf-ops-automation-bosh-broker-0.27.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

