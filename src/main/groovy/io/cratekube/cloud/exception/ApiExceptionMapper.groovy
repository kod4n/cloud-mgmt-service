package io.cratekube.cloud.exception

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

import static javax.ws.rs.client.Entity.json

/**
 * Exception mapper for all {@link ApiException} throwables.
 */
@Provider
class ApiExceptionMapper implements ExceptionMapper<ApiException> {
  @Override
  Response toResponse(ApiException exception) {
    return Response.status(exception.errorCode)
      .entity(json(message: exception.message))
      .build()
  }
}
