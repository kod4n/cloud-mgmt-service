package io.cratekube.cloud.api

import groovy.transform.InheritConstructors
import io.cratekube.cloud.exception.NotAcceptableException
import io.cratekube.cloud.exception.NotFoundException
import io.cratekube.cloud.model.Environment
import io.cratekube.cloud.resources.EnvironmentResource.EnvironmentRequest

/**
 * Interface for managing interactions with environments
 */
interface EnvironmentManager {
  /**
   * Starts the creation process for an environment.  Returns the initial object representation of the
   * environment that will be created.
   *
   * @param environmentRequest {@code non-null} request object
   * @return {@code non-null} environment object
   * @throws EnvironmentAlreadyExistsException
   */
  Environment create(EnvironmentRequest environmentRequest) throws EnvironmentAlreadyExistsException

  /**
   * Finds all environments that have been requested for provisioning.
   *
   * @return list of environments, if none are found returns an empty list
   */
  List<Environment> getAll()

  /**
   * Finds a specific environment by name.
   *
   * @param environmentName {@code non-empty} environment to lookup
   * @return {@link Environment} object if found, otherwise return empty optional
   */
  Optional<Environment> findByName(String environmentName)

  /**
   * Deletes an environment by name.
   *
   * @param environmentName {@code non-empty} name of environment
   * @throws EnvironmentNotFoundException
   */
  void deleteByName(String environmentName) throws EnvironmentNotFoundException
}

@InheritConstructors class EnvironmentAlreadyExistsException extends NotAcceptableException {}
@InheritConstructors class EnvironmentNotFoundException extends NotFoundException {}
