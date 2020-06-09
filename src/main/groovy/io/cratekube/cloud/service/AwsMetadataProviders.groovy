package io.cratekube.cloud.service

import io.cratekube.cloud.api.ResourceMetadataProvider

/**
 * Helper class for accessing metadata providers for different AWS terraform types.
 * These metadata providers should be used to populate metadata when converting terraform
 * state into {@link io.cratekube.cloud.model.ManagedResource} objects.
 */
class AwsMetadataProviders {
  static final ResourceMetadataProvider DEFAULT_METADATA_PROVIDER = (attr) -> [:]

  static final ResourceMetadataProvider EC2_METADATA_PROVIDER = (Map<String, Object> attributes) -> [
    publicDns: attributes.public_dns,
    publicIp: attributes.public_ip
  ]

  static ResourceMetadataProvider getMetadataProvider(String metadataType) {
    switch (metadataType) {
      case 'aws_instance':
        EC2_METADATA_PROVIDER
        break
      default:
        DEFAULT_METADATA_PROVIDER
    }
  }
}
