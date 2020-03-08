package io.cratekube.cloud.resources

import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.model.Environment
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam

import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * JAX-RS resource for managing CrateKube environments.  This API is the main interface for generating cloud resources
 * for CrateKube environments.
 */
@Slf4j
@Api('environments')
@Path('environment')
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class EnvironmentResource {
  EnvironmentManager environments

  @Inject
  EnvironmentResource(EnvironmentManager environments) {
    this.environments = require environments, notNullValue()
  }

  /**
   * Lists all environments and their provisioned resources.
   *
   * @return list of environments, can be empty
   */
  @GET
  List<Environment> getEnvironments() {
    log.debug '[list-env] listing all environments'
    return []
  }

  /**
   * Finds a specific environment by name.  If no environment is found a 404 response will
   * be returned.
   *
   * @param environmentName {@code non-empty} environment name
   * @return the found environment, otherwise a 404 response
   */
  @GET
  @Path('{environmentId}')
  Environment getEnvironmentByName(@PathParam('environmentName') String environmentName) {
    require environmentName, notEmptyString()
    log.debug '[get-env-by-id] getting environment {}', environmentName
    return null
  }

  /**
   * Creates a new environment using the provided request object.
   *
   * @param envRequest {@code non-null} request object
   * @return
   */
  @POST
  @Path('{environmentId}')
  Environment createEnvironment(@ApiParam EnvironmentRequest envRequest) {
    require envRequest, notNullValue()
    log.debug '[create-env] creating environment {}', envRequest
    return null
  }

  /**
   * Deletes and environment by name.  If the environment was successfully removed a 201
   * response will be returned.
   *
   * @param environmentName {@code non-empty} environment name
   * @return 201 response on successful deletion
   */
  @DELETE
  @Path('{environmentId}')
  Response deleteEnvironmentByName(@PathParam('environmentName') String environmentName) {
    require environmentName, notEmptyString()
    log.debug '[delete-env-by-id] deleting environment {}', environmentName
    return null
  }

  /**
   * Request object for creating new environments
   */
  @Immutable
  static class EnvironmentRequest {
    /**
     * Name of the environment to create
     */
    String name
  }
}
