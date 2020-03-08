package io.cratekube.cloud

import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration

import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Configuration class for this Dropwizard application.
 */
class AppConfig extends Configuration {
  JerseyClientConfiguration jerseyClient

  /**
   * Represents the cloud this service is operating in.
   * Currently, this will only be {@code aws}.
   */
  @NotEmpty
  String provider

  @Valid
  @NotNull
  SwaggerBundleConfiguration swagger
}
