package io.cratekube.cloud.api

/**
 * This interface provides metadata for resources based on a map of attributes.
 * Designed as a single abstract method interface so that implementing providers
 * can be created as inline lambdas.
 */
interface ResourceMetadataProvider {
  Map<String, Object> getMetadata(Map<String, Object> attributes)
}
