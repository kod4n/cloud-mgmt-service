package io.cratekube.cloud

import io.cratekube.auth.User

final class Fixtures {
  public static final String TEST_ENV = 'test-env'
  public static final String TEST_APIKEY = 'test-token'
  public static final User TEST_USER = new User(name: 'test-user')
}
