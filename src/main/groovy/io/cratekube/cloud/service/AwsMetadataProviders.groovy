package io.cratekube.cloud.service

import io.cratekube.cloud.api.ResourceMetadataProvider
import io.cratekube.cloud.model.ResourceMetadata
import io.cratekube.cloud.model.aws.Ec2Metadata

/**
 * Helper class for accessing metadata providers for different AWS terraform types.
 * These metadata providers should be used to parse subclasses of {@link ResourceMetadata} when converting terraform
 * state into {@link io.cratekube.cloud.model.ManagedResource} objects.
 */
class AwsMetadataProviders {
  static final ResourceMetadataProvider<Ec2Metadata> ec2MetadataProvider = (Map<String, Object> attributes) -> new Ec2Metadata(
    publicDns: attributes.public_dns,
    publicIp: attributes.public_ip
  )
  static final ResourceMetadataProvider<ResourceMetadata> defaultMetadataProvider = (attr) -> null

  static ResourceMetadataProvider getMetadataProvider(String metadataType) {
    switch (metadataType) {
      case 'aws_instance':
        ec2MetadataProvider
        break
      default:
        defaultMetadataProvider
    }
  }
}
