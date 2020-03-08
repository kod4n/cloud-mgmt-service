package io.cratekube.cloud.model

import groovy.transform.Immutable

@Immutable
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
   * Current status of the resource.  This property is helpful in determining when
   * resources are available for use.
   */
  Status status
}
