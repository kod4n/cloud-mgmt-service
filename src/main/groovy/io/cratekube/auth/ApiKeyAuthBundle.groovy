package io.cratekube.auth

import io.dropwizard.ConfiguredBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.setup.Environment
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature

import java.security.Principal

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Bundle used for api key authc/authz.
 *
 * @param <C> the configuration type
 * @param <P> the principal type
 */
class ApiKeyAuthBundle<C, P extends Principal> implements ConfiguredBundle<C> {
  Class<P> userClass
  AuthConfigProvider<C> authConfigProvider

  ApiKeyAuthBundle(AuthConfigProvider<C> authConfigProvider) {
    this(User, authConfigProvider)
  }

  ApiKeyAuthBundle(Class<P> userClass, AuthConfigProvider<C> authConfigProvider) {
    this.userClass = checkNotNull userClass
    this.authConfigProvider = checkNotNull authConfigProvider
  }

  @Override
  void run(C configuration, Environment environment) throws Exception {
    environment.jersey().with {
      register new AuthDynamicFeature(createAuthFilter(configuration))
      register RolesAllowedDynamicFeature
      register new AuthValueFactoryProvider.Binder(userClass)
    }
  }

  /**
   * Builds an OAuth filter using the bearer token authentication method.  The auth configuration from the
   * provider will need to have an API key object in order for authentication to work when using {@code @Auth} and
   * {@code @RolesAllowed} annotations.
   *
   * @param config the Dropwizard configuration object
   * @return the created auth filter
   */
  private OAuthCredentialAuthFilter<P> createAuthFilter(C config) {
    def authConfig = authConfigProvider.get(config)
    def apiKeyProvider = (String key) -> authConfig.apiKeys.find { it.key == key }
    def userFactory = (ApiKey apiKey) -> apiKey.with { new User(name, roles) }
    def apiKeyNameProvider = (String name) -> authConfig.apiKeys.find { it.name == name }
    return new OAuthCredentialAuthFilter.Builder<P>().with {
      prefix = 'Bearer'
      authenticator = new ApiKeyAuthenticator<>(apiKeyProvider, userFactory)
      authorizer = new ApiKeyAuthorizer<>(apiKeyNameProvider)
      buildAuthFilter()
    }
  }
}
