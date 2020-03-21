package io.cratekube.cloud.model

import groovy.transform.Immutable

/**
 * Represents a set of namespaced resources for a given cloud-provider.
 */
@Immutable
class Environment {
  /**
   * Internal identifier for this environment.
   */
  UUID id

  /**
   * Name of the environment.
   */
  String name

  /**
   * Cloud provider used to provision environment resources.
   * Currently, this will only be {@code aws}
   */
  String provider

  /**
   * Status for the environment
   */
  Status status

  /**
   * List of managed resources for this environment.
   */
  List<ManagedResource> resources
}
