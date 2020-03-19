package io.cratekube.cloud

import io.cratekube.auth.User
import io.cratekube.cloud.resources.EnvironmentResource.EnvironmentRequest

final class Fixtures {
  public static final String TEST_ENV = 'test-env'
  public static final String TEST_APIKEY = 'test-token'
  public static final User TEST_USER = new User(name: 'test-user')

  static EnvironmentRequest envRequest(Map options) {
    return new EnvironmentRequest(name: options.name ?: TEST_ENV)
  }
}
