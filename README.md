[![License](http://img.shields.io/badge/license-APACHE-blue.svg?style=flat)](http://choosealicense.com/licenses/apache-2.0/)
[![SemVer](http://img.shields.io/badge/semver-2.0.0-blue.svg?style=flat)](http://semver.org/spec/v2.0.0)
[![Download](https://api.bintray.com/packages/cratekube/maven/cloud-mgmt-service-client/images/download.svg)](https://bintray.com/cratekube/maven/cloud-mgmt-service-client/_latestVersion)
[![Build Status](https://travis-ci.com/cratekube/cloud-mgmt-service.svg?branch=master)](https://travis-ci.com/cratekube/cloud-mgmt-service)
[![Coverage Status](https://coveralls.io/repos/github/cratekube/cloud-mgmt-service/badge.svg?branch=master)](https://coveralls.io/github/cratekube/cloud-mgmt-service?branch=master)

_A service to manage and monitor assets in cloud platforms_

## Introduction
If you are unfamiliar with [CrateKube](https://github.com/cratekube/cratekube/blob/master/docs/Architecture.md), please read our [User Documentation for the Cloud Management Service](https://github.com/cratekube/cratekube/blob/master/docs/user/ServiceCloudManagement.md).  This microservice _can_ operate as an independent API, however it is designed as part of a [larger system](https://github.com/cratekube/cratekube/blob/master/docs/Architecture.md) that addresses the following [requirements](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md) in order to provide a hybrid-cloud management platform that is [default-secure](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#automatic-container-hardening) and [ephemeral](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#read-only-container-file-systems) with [elastic provisioning](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#elastic-provisioning), [hardened pod security policies](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#pod-security-policies), self-healing [event-driven architecture](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#event-based-control-plane-automation), and [persistent EBS backed storage](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md#persistent-storage).

## Quick links
- [User documentation](https://github.com/cratekube/cratekube/blob/master/docs/user/ServiceCloudManagement.md) for the `cloud-mgmt-service`
- CrateKube [operator documentation](http://cratekube.github.io)
- [System Architecture](https://github.com/cratekube/cratekube/blob/master/docs/Architecture.md) and high-level [Requirements](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md)
- [How to contribute](https://github.com/cratekube/cratekube/blob/master/CONTRIBUTING.md) 
- Contributor [architecture](https://github.com/cratekube/cratekube/blob/master/contributing/Architecture%20Guidelines.md), [roles and responsibilities](https://github.com/cratekube/cratekube/blob/master/docs/RolesAndResponsibilities.md), and [developer documentation](https://github.com/cratekube/cratekube/blob/master/docs/Development.md)

## How this app works
This application has terraform [files](src/main/resources/terraform) and [templates](src/main/resources/terraform/templates)
that can be found in the resources directory for the project.  These terraform files are used to initialize terraform
directories when environments are created.

After the required terraform files are created an environment directory is initialized with `terraform init`.  A plan is
generated for all the required resources and and then applied via terraform.  Accessing state for environments is accomplished
using `terraform state` calls.  Destroying environments is accomplished through `terraform destroy`.

In order for terraform calls to be successful environment variables for `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
must be provided when running the application.

## Configuration
Internal and external services are configured by extending the Dropwizard application configuration with additional
settings. An environment variable parser is used to allow configuration settings to be overridden at runtime. 
These configuration options can be seen in the [app config file](app.yml).

## Local development

### Gradle builds
This project uses [gradle](https://github.com/gradle/gradle) for building and testing.  We also use the gradle wrapper
to avoid downloading a local distribution.  The commands below are helpful for building and testing.
- `./gradlew build` compile and build the application
- `./gradlew check` run static code analysis and test the application
- `./gradlew shadowJar` builds a fat jar that can be used to run the Dropwizard application
- `./gradlew buildClient` generates the API client code for the Dropwizard application
- `./gradlew publishToMavenLocal` publishes any local artifacts to the local .m2 repository

After you have generated the fat jar you can run your application with java using:
```bash
java -jar build/libs/cloud-mgmt-service-1.0.0-SNAPSHOT-all.jar
```

### Docker builds
We strive to have our builds repeatable across development environments so we also provide a Docker build to generate 
the Dropwizard application container.  The examples below should be executed from the root of the project.

Run the base docker build:
```bash
docker build -t cloud-mgmt-service:build --target build .
```
Note: This requires docker 19.03.x or above.  Docker 18.09 will throw errors for mount points and the `--target` flag.

Build the package target:
```
docker build -t cloud-mgmt-service:package --target package .
```
Run the docker application locally on port 8080:
```bash
docker run -p 8080:9000 -d cloud-mgmt-service:package
```

Fire up the Swagger specification by visiting the following URL in a browser:
```bash
http://localhost:8080/swagger
```

## Using the API client
This application generates a client for the Dropwizard application by using the swagger specification.  The maven asset
is available in JCenter, make sure you include the JCenter repository (https://jcenter.bintray.com/) when pulling this
client.  To use the client provide the following dependency in your project:

Gradle:
```groovy
implementation 'io.cratekube:cloud-mgmt-service:1.0.0'
``` 

Maven:
```xml
<dependency>
  <groupId>io.cratekube</groupId>
  <artifactId>cloud-mgmt-service</artifactId>
  <version>1.0.0</version>
</dependency>
```

## API Documentation
The API docs for this project are powered by the Swagger Specification. After starting up the application the available
APIs can be found at `http://localhost:<configured port>/swagger`

## Contributing
If you are interested in contributing to this project please review the [contribution guidelines](https://github.com/cratekube/cratekube/blob/master/CONTRIBUTING.md).
Thank you for your interest in CrateKube!
