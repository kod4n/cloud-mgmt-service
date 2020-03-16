package io.cratekube.auth

import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator

import java.security.Principal

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Default {@link Authenticator} implementation for the api key auth bundle.
 * @param <P> the principal type
 */
class ApiKeyAuthenticator<P extends Principal> implements Authenticator<String, P> {
  ApiKeyProvider apiKeyProvider
  UserFactory<ApiKey, P> userFactory

  ApiKeyAuthenticator(ApiKeyProvider apiKeyProvider, UserFactory<ApiKey, P> userFactory) {
    this.apiKeyProvider = checkNotNull apiKeyProvider
    this.userFactory = checkNotNull userFactory
  }

  @Override
  Optional<P> authenticate(String credentials) throws AuthenticationException {
    def apiKey = apiKeyProvider.get(credentials)
    return Optional.ofNullable(apiKey).map(key -> userFactory.create(key))
  }
}
