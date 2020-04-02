package io.cratekube.cloud.model

import groovy.transform.Canonical

@Canonical
class ManagedResource {
  /**
   * Identifier for the resource
   */
  String id

  /**
   * Name for the resource
   */
  String name

  /**
   * Type for the resource
   */
  String type

  /**
   * Current status of the resource.  This property is helpful in determining when
   * resources are available for use.
   */
  Status status

  /**
   * Configuration of the managed resource
   */
  ResourceMetadata metadata
}
