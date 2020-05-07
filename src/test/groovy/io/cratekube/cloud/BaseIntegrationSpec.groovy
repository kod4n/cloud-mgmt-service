package io.cratekube.cloud

import org.spockframework.mock.MockUtil
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.HttpHeaders

/**
 * Base class for all integration specs.  This class provides a client for interacting with the
 * Dropwizard application's API.
 */
@UseDropwizardApp(
  value = App,
  hooks = IntegrationSpecHook,
  config = 'app.yml',
  configOverride = [
    @ConfigOverride(key = 'auth.api-keys[0].key', value = Fixtures.TEST_APIKEY),
    @ConfigOverride(key = 'jerseyClient.timeout', value = '5000ms'),
    @ConfigOverride(key = 'jerseyClient.gzipEnabledForRequests', value = 'false'),
    @ConfigOverride(key = 'service.sshPublicKeyPath', value = 'test-ssh-key-path'),
  ]
)
abstract class BaseIntegrationSpec extends Specification {
  @Inject Client client

  MockUtil mockUtil = new MockUtil()

  def setup() {
    requiredMocks.each { mockUtil.attachMock(it, this) }
  }

  /**
   * Base path used for API requests. Can be overridden by classes extending this spec.
   *
   * @return the base API path for requests
   */
  abstract String getBaseRequestPath()

  /**
   * List of mocked objects that should be attached to this specification.
   * Attaching of required mocks happens during the {@code setup} method for this specification.
   *
   * @return list of interfaces to attach
   */
  protected List<Object> getRequiredMocks() {
    return []
  }

  /**
   * Creates a client invocation builder using the provided path.
   *
   * @param path {@code non-null} api path to call
   * @return an {@link Invocation.Builder} instance for the request
   */
  protected Invocation.Builder baseRequest(String path = '') {
    return client.target("http://localhost:9000${baseRequestPath}${path}")
      .request()
      .header(HttpHeaders.AUTHORIZATION, "Bearer ${Fixtures.TEST_APIKEY}")
  }
}
