package io.cratekube.cloud.resources

import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import io.cratekube.auth.ApiAuth
import io.cratekube.auth.User
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.model.Environment
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
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
  EnvironmentManager envManager

  @Inject
  EnvironmentResource(EnvironmentManager envManager) {
    this.envManager = require envManager, notNullValue()
  }

  /**
   * Lists all environments and their provisioned resources.
   *
   * @return list of environments, can be empty
   */
  @GET
  @ApiImplicitParams(
    @ApiImplicitParam(name = 'Authorization', value = 'API token', required = true, dataType = 'string', paramType = 'header')
  )
  List<Environment> getEnvironments(@ApiAuth User user) {
    log.debug '[list-env] user [{}] listing all environments', user.name
    return envManager.all
  }

  /**
   * Creates a new environment using the provided request object.
   * If no exception is thrown a 202 accepted response will be returned with the location
   * for the resource.
   *
   * @param envRequest {@code non-null} request object
   * @return 202 accepted response given no exception, otherwise a 4xx/5xx response depending on the exception
   */
  @POST
  @ApiImplicitParams(
    @ApiImplicitParam(name = 'Authorization', value = 'API token', required = true, dataType = 'string', paramType = 'header')
  )
  Response createEnvironment(@ApiAuth User user, @ApiParam EnvironmentRequest envRequest) {
    require envRequest, notNullValue()
    log.debug '[create-env] user [{}] creating environment {}', user.name, envRequest
    def env = envManager.create(envRequest)
    return Response.accepted().location("/environment/${env.name}".toURI()).build()
  }

  /**
   * Finds a specific environment by name.  If no environment is found a 404 response will
   * be returned.
   *
   * @param environmentName {@code non-empty} environment name
   * @return the found environment, otherwise a 404 response
   */
  @GET
  @Path('{environmentName}')
  @ApiOperation(value = 'getEnvironmentByName', response = Environment)
  @ApiImplicitParams(
    @ApiImplicitParam(name = 'Authorization', value = 'API token', required = true, dataType = 'string', paramType = 'header')
  )
  Optional<Environment> getEnvironmentByName(@ApiAuth User user, @PathParam('environmentName') String environmentName) {
    require environmentName, notEmptyString()
    log.debug '[get-env-by-id] user [{}] getting environment {}', user.name, environmentName
    return envManager.findByName(environmentName)
  }

  /**
   * Deletes and environment by name.  If the environment was successfully removed a 202
   * response will be returned.
   *
   * @param environmentName {@code non-empty} environment name
   * @return 202 response on successful delete operation
   */
  @DELETE
  @Path('{environmentName}')
  @ApiImplicitParams(
    @ApiImplicitParam(name = 'Authorization', value = 'API token', required = true, dataType = 'string', paramType = 'header')
  )
  Response deleteEnvironmentByName(@ApiAuth User user, @PathParam('environmentName') String environmentName) {
    require environmentName, notEmptyString()
    log.debug '[delete-env-by-id] user [{}] deleting environment {}', user.name, environmentName
    envManager.deleteByName(environmentName)
    return Response.accepted().build()
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
