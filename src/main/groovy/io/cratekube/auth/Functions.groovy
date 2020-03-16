package io.cratekube.auth

import java.security.Principal

@FunctionalInterface
interface ApiKeyProvider {
  ApiKey get(String value)
}

@FunctionalInterface
interface UserFactory<T, P extends Principal> {
  P create(T credential)
}

@FunctionalInterface
interface AuthConfigProvider<C> {
  AuthConfig get(C config)
}
