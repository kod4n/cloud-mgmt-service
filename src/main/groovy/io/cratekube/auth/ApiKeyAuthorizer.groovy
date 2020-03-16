package io.cratekube.auth

import io.dropwizard.auth.Authorizer

import java.security.Principal

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Default {@link io.dropwizard.auth.Authorizer} implementation for the api key auth bundle.
 * @param <P> the principal type
 */
class ApiKeyAuthorizer<P extends Principal> implements Authorizer<P> {
  ApiKeyProvider apiKeyProvider

  ApiKeyAuthorizer(ApiKeyProvider apiKeyProvider) {
    this.apiKeyProvider = checkNotNull apiKeyProvider
  }

  @Override
  boolean authorize(P principal, String role) {
    def apiKey = apiKeyProvider.get(principal.name)
    return role in apiKey?.roles
  }
}
