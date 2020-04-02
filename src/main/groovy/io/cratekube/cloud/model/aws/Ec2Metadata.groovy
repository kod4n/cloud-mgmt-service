package io.cratekube.cloud.model.aws

import groovy.transform.Canonical
import io.cratekube.cloud.model.ResourceMetadata

@Canonical
class Ec2Metadata implements ResourceMetadata {
  String publicDns
  String publicIp
}
