package io.cratekube.cloud

import groovy.transform.Immutable
import io.cratekube.auth.AuthConfig
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

  @Valid
  @NotNull
  AuthConfig auth

  @Valid
  @NotNull
  ServiceConfig service

  @Valid
  @NotNull
  SwaggerBundleConfiguration swagger
}

@Immutable
class ServiceConfig {
  /**
   * Represents the cloud this service is operating in.
   * Currently, this will only be {@code aws}.
   */
  @NotEmpty
  String provider

  /**
   * Path to configuration directory.  This directory is used to store terraform state
   * for environments.
   */
  @NotEmpty
  String configDir

  /**
   * Value of the ssh public key to be used
   */
  @NotEmpty
  String sshPublicKey
}
