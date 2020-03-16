package io.cratekube.auth

import io.cratekube.cloud.Fixtures
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static spock.util.matcher.HamcrestSupport.expect

class ApiKeyAuthenticatorSpec extends Specification {
  @Subject ApiKeyAuthenticator<User> subject
  ApiKeyProvider apiKeyProvider
  UserFactory<ApiKey, User> userFactory

  def setup() {
    apiKeyProvider = Stub()
    userFactory = Stub()
    subject = new ApiKeyAuthenticator(apiKeyProvider, userFactory)
  }

  def 'should require valid constructor parameters'() {
    when:
    new ApiKeyAuthenticator(keyProvider, usrFactory)

    then:
    thrown NullPointerException

    where:
    keyProvider         | usrFactory
    null                | null
    this.apiKeyProvider | null
  }

  def 'should return empty optional when apikey provider returns a null'() {
    given:
    def apiKey = 'test-key'
    apiKeyProvider.get(apiKey) >> null

    when:
    def result = subject.authenticate(apiKey)

    then:
    expect result.isPresent(), equalTo(false)
  }

  def 'should return populate optional when apikey provider returns an object'() {
    given:
    def keyValue = 'test-key'
    def apiKey = new ApiKey(name: 'test-key', key: keyValue, roles: [])
    apiKeyProvider.get('test-key') >> apiKey
    userFactory.create(apiKey) >> Fixtures.TEST_USER

    when:
    def result = subject.authenticate(keyValue)

    then:
    expect result.isPresent(), equalTo(true)
    verifyAll(result.get()) {
      expect name, equalTo('test-user')
      expect roles, empty()
    }
  }
}
