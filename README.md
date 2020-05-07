[![License](http://img.shields.io/badge/license-APACHE-blue.svg?style=flat)](http://choosealicense.com/licenses/apache-2.0/)
[![SemVer](http://img.shields.io/badge/semver-2.0.0-blue.svg?style=flat)](http://semver.org/spec/v2.0.0)
[![Download](https://api.bintray.com/packages/cratekube/maven/cloud-mgmt-service-client/images/download.svg)](https://bintray.com/cratekube/maven/cloud-mgmt-service-client/_latestVersion)
[![Build Status](https://travis-ci.com/cratekube/cloud-mgmt-service.svg?branch=master)](https://travis-ci.com/cratekube/cloud-mgmt-service)
[![Coverage Status](https://coveralls.io/repos/github/cratekube/cloud-mgmt-service/badge.svg?branch=master)](https://coveralls.io/github/cratekube/cloud-mgmt-service?branch=master)

_A service to manage and monitor assets in cloud platforms_

## Introduction
This **_cloud management service_** is part of an [MVaP architecture](https://github.com/cratekube/cratekube/blob/master/docs/Architecture.md) and set of [requirements](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md) for [CrateKube](https://cratekube.github.io/) that creates infrastructure [VPC](https://aws.amazon.com/vpc/)s, bootstraps, and configures [Kubernetes](https://kubernetes.io/) clusters on AWS [EC2](https://aws.amazon.com/ec2/pricing/) using [CloudFormation](https://aws.amazon.com/cloudformation/) templates and [Terraform](https://www.terraform.io/).  The underlying objective of our product is to provide default secure, ephemeral, cloud-ready instances that will launch on [AWS](https://aws.amazon.com/ec2/).  

## Quick links
- CrateKube [website](http://cratekube.github.io)
- [System architecture](https://github.com/cratekube/cratekube/blob/master/docs/Architecture.md) and high-level [requirements](https://github.com/cratekube/cratekube/blob/master/docs/Requirements.md)
- [How to contribute](https://github.com/cratekube/cratekube/blob/master/CONTRIBUTING.md) 
- Contributor [architecture](https://github.com/cratekube/cratekube/blob/master/contributing/Architecture%20Guidelines.md), [roles and responsibilities](https://github.com/cratekube/cratekube/blob/master/docs/RolesAndResponsibilities.md), and [developer documentation](https://github.com/cratekube/cratekube/blob/master/docs/Development.md)

## What does this service do?
The cloud-mgmt-service is in charge of provisioning and monitoring all cloud resources and services. Cloud resources are loosely defined as managed virtual infrastructure, or "Infrastructure as code".

In AWS, this is expected to provision any and all resources necessary to prepare for the creation of a [Kubernetes cluster](https://kubernetes.io/docs/tutorials/kubernetes-basics/create-cluster/), including but not limited to [IAM](https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html), [VPC](https://docs.aws.amazon.com/vpc/latest/userguide/what-is-amazon-vpc.html), [EC2 instances](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/concepts.html), [subnets](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Subnets.html#vpc-subnet-basics), [security groups](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html), and security policies. 

When utilized as a component, this service can act as a stand-alone service for creating infrastructure as code, and can be easily extended by forking and [developing](https://github.com/cratekube/cratekube/blob/master/docs/Development.md) as a [CrateKube contributor](https://github.com/cratekube/cratekube/blob/master/CONTRIBUTING.md).


# Development 

## Configuration
The DropWizard [app.yml](https://github.com/cratekube/cloud-mgmt-service/blob/master/app.yml) can be configured dynamically using environment variables:

Example environment variable file:

```html
$ CONFIG_DIR=/app/config
$ SSH_PUBLIC_KEY_PATH=<public key path>
$ ADMIN_APIKEY=<api key>
$ AWS_ACCESS_KEY_ID=<value>
$ AWS_SECRET_ACCESS_KEY=<value>

```
CONFIG_DIR specifies the path to configuration directory.  This directory is used to store terraform state for environments.

SSH_PUBLIC_KEY_PATH is the path to the public key for the host where this service is deployed.

ADMIN_APIKEY is the bearer token that will be used to authenticate CrateKube platform services. 

The AWS keys are your AWS ID and KEY with permissions to configure a full VPC

These environment variables are the preferred method of configuration at runtime.

## Quickstart
To run this service, simply execute:
```bash
docker run -p 8080:9000 --env-file /path/to/envfile -v /home/user/tfstate:/app/config -d cratekube/cloud-mgmt-service
```

## How this app works
This application has terraform [files](src/main/resources/terraform) and [templates](src/main/resources/terraform/templates)
that can be found in the resources directory for the project.  These terraform files are used to initialize terraform
directories when environments are created.

After the required terraform files are created an environment directory is initialized with `terraform init`.  A plan is
generated for all the required resources and and then applied via terraform.  Accessing state for environments is accomplished
using `terraform state` calls.  Destroying environments is accomplished through `terraform destroy`.

In order for terraform calls to be successful environment variables for `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
must be provided when running the application.

### Building with Docker
We strive to have our builds repeatable across development environments so we also provide a Docker build to generate 
the Dropwizard application container.  The examples below should be executed from the root of the project.

##### Run the base docker build:
```bash
docker build -t cloud-mgmt-service:local --target build .
```
Note: This requires docker 19.03.x or above.  Docker 18.09 will throw errors for mount points and the `--target` flag.

##### Build the package target:
```bash
docker build -t cloud-mgmt-service:local --target package .
```

##### Run the docker application locally on port 8080:
```bash
docker run -p 8080:9000 -v /home/user/tfstate:/app/config -d cloud-mgmt-service:local
```
Note: We are bind mounting the `/home/user/tfstate` directory inside of the container to preserve the state of the Terraform installation locally on the host.  If you don't do that, whenever your container dies you will lose your infrastructure state files.  **That's bad!**  _Without state, the next time Terraform runs it could remove infrastructure it doesn't know it exists._

If you don't want to preserve state, you can just remove the volume flag.

##### Fire up the Swagger specification by visiting the following URL in a browser:
```bash
http://localhost:8080/swagger
```

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

# Running the app

### Running on AWS
This microservice can also run on an Amazon EC2 instance:
- Find an [Ubuntu Cloud AMI](https://cloud-images.ubuntu.com/locator/ec2/) and spin it up in your AWS EC2 console.
- [Install Docker](https://docs.docker.com/engine/install/ubuntu/) 
- [Push](https://docs.docker.com/docker-hub/) your locally built docker image to a registry
- [Configure](https://github.com/cratekube/cloud-mgmt-service#configuration) the necessary environment variables
- Set up your [EC2 security groups](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html) to protect this app from the outside world
- Pull the docker image from dockerhub i.e.- `docker pull yourUser/yourImage:tag`
- [Run](https://github.com/cratekube/cloud-mgmt-service#building-with-docker) the container

The cloud management service will become available at:
```html
http://<ec2 instance public dns name>:8080/swagger
```
### Requirements for deployment on AWS
- Requires SuperUser permission to an AWS account using the IAM keys of your choice.   
- Terraform templates will need to have configuration options that support the compliance rules implemented in the [policy-mgmt-service](https://github.com/cratekube/policy-mgmt-service). 
- The state files generated by Terraform must be persisted.  If running this service on EC2, you must persist the data in a volume in order for Docker to attach to it and save the state.

## Using the API
The API has endpoints that allow you to create, read, and delete environments.  In order for these operations to be successful, your app.yml file must have been properly configured to utilize your AWS account with the appropriate key.  That API key must have access to all the resources necessary to configure a full VPC.

The resulting operations exist as REST endpoints, which you can simply hit in your browser or with a tool such as [Postman](https://www.postman.com/downloads/).

| HTTP Verb | Endpoint | Payload | Authentication| Function |
| --- | --- | --- | --- | --- |
| GET | /environment | None | API Bearer Token | Get a list of all environments |
| POST | /environment | <code>{ "name": "string"}</code>  | API Bearer Token | Create an environment by specifying a name |
| GET | /environment/{environmentName} | None | None | Get a specific environment by name |
| DELETE | /environment/{environmentName} | None | None | Delete a specific environment by name |

## API Documentation
The API docs for this project are powered by the Swagger Specification. After starting up the application the available
APIs can be found at `http://localhost:<configured port>/swagger`

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

## Contributing
If you are interested in contributing to this project please review the [contribution guidelines](https://github.com/cratekube/cratekube/blob/master/CONTRIBUTING.md).
Thank you for your interest in CrateKube!
