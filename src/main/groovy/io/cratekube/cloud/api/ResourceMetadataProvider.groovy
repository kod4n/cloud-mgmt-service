package io.cratekube.cloud.api

import io.cratekube.cloud.model.ResourceMetadata

/**
 * This interface provides metadata for resources based on a map of attributes.
 * Designed as a single abstract method interface so that implementing providers
 * can be created as inline lambdas.
 *
 * @param <T> metadata type extending {@link ResourceMetadata}
 */
interface ResourceMetadataProvider<T extends ResourceMetadata> {
  T getMetadata(Map<String, Object> attributes)
}
