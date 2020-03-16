package io.cratekube.auth

import io.cratekube.cloud.Fixtures
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static spock.util.matcher.HamcrestSupport.expect

class ApiKeyAuthorizerSpec extends Specification {
  @Subject ApiKeyAuthorizer<User> subject
  ApiKeyProvider apiKeyProvider

  def setup() {
    apiKeyProvider = Stub()
    subject = new ApiKeyAuthorizer(apiKeyProvider)
  }

  def 'should require valid constructor parameters'() {
    when:
    new ApiKeyAuthorizer(null)

    then:
    thrown NullPointerException
  }

  def 'should reject authorization when provider returns null apikey'() {
    given:
    apiKeyProvider.get(_) >> null

    when:
    def result = subject.authorize(Fixtures.TEST_USER, 'test-role')

    then:
    expect result, equalTo(false)
  }

  def 'should reject authorization when apikey does not contain role'() {
    given:
    apiKeyProvider.get(_) >> new ApiKey(name: 'test-key', key: 'key-value', roles: ['different-role'])

    when:
    def result = subject.authorize(Fixtures.TEST_USER, 'test-role')

    then:
    expect result, equalTo(false)
  }

  def 'should accept authorization when apikey contains role'() {
    given:
    def role = 'test-role'
    apiKeyProvider.get(_) >> new ApiKey(name: 'test-key', key: 'key-value', roles: [role])

    when:
    def result = subject.authorize(Fixtures.TEST_USER, role)

    then:
    expect result, equalTo(true)
  }
}
