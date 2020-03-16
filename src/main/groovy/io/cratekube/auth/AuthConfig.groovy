package io.cratekube.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import groovy.transform.Immutable

import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * Configuration object for the auth bundle.
 */
@Immutable
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy)
class AuthConfig {
  @Valid
  @NotEmpty
  List<ApiKey> apiKeys
}

/**
 * Represents an api key configuration object.  Each object should have the {@code name} and {@code property} values
 * populated with {@code non-empty} string values.  The {@code roles} property can be omitted and will be replaced
 * with an empty set.
 */
@Immutable
class ApiKey {
  @NotEmpty
  String name

  @NotEmpty
  String key

  Set<String> roles = [] as Set<String>
}
