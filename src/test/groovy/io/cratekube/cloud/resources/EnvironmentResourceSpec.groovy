package io.cratekube.cloud.resources

import io.cratekube.auth.User
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.model.Environment
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.core.Response

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class EnvironmentResourceSpec extends Specification {
  @Subject EnvironmentResource resource

  EnvironmentManager environmentManager

  static final String ENV_NAME = 'test-env'
  static final User TEST_USER = new User(name: 'test-user')

  def setup() {
    environmentManager = Mock(EnvironmentManager)
    resource = new EnvironmentResource(environmentManager)
  }

  def 'should require valid constructor params'() {
    given:
    def envs = null

    when:
    new EnvironmentResource(envs)

    then:
    thrown RequireViolation
  }

  def 'should return empty list from manager'() {
    given:
    environmentManager.all >> []

    when:
    def result = resource.getEnvironments(TEST_USER)

    then:
    expect result, allOf(notNullValue(), empty())
  }

  def 'should return populated list when manager has results'() {
    given:
    environmentManager.all >> [new Environment(name: 'test-env')]

    when:
    def result = resource.getEnvironments(TEST_USER)

    then:
    verifyAll(result) {
      expect it, hasSize(1)
      expect first().name, equalTo('test-env')
    }
  }

  def 'should require valid parameters for createEnvironment'() {
    when:
    resource.createEnvironment(user, envReq)

    then:
    thrown RequireViolation

    where:
    user      | envReq
    null      | null
    TEST_USER | null
  }

  def 'should return accepted response when manager creates environment'() {
    given:
    def env = new Environment(name: ENV_NAME)
    environmentManager.create(hasProperty('name', equalTo(ENV_NAME))) >> env

    when:
    def result = resource.createEnvironment(TEST_USER, new EnvironmentResource.EnvironmentRequest(ENV_NAME))

    then:
    expect result, notNullValue()
    expect result.status, equalTo(Response.Status.ACCEPTED.statusCode)
  }

  def 'should require valid parameters for getEnvironmentById'() {
    when:
    resource.getEnvironmentByName(user, envName)

    then:
    thrown RequireViolation

    where:
    user      | envName
    null      | null
    TEST_USER | null
    TEST_USER | ''
  }

  def 'should return present optional when manager returns a result'() {
    given:
    def env = new Environment(name: ENV_NAME)
    environmentManager.findByName(_) >> Optional.of(env)

    when:
    def result = resource.getEnvironmentByName(TEST_USER, ENV_NAME)

    then:
    verifyAll(result) {
      expect present, equalTo(true)
      expect get(), notNullValue()
      expect get().name, equalTo(ENV_NAME)
    }
  }

  def 'should return empty optional when manager returns null'() {
    given:
    environmentManager.findByName(_) >> Optional.empty()

    when:
    def result = resource.getEnvironmentByName(TEST_USER, ENV_NAME)

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect present, equalTo(false)
    }
  }

  def 'should require valid parameters for deleteEnvironmentById'() {
    when:
    resource.deleteEnvironmentByName(user, envName)

    then:
    thrown RequireViolation

    where:
    user      | envName
    null      | null
    TEST_USER | null
    TEST_USER | ''
  }

  def 'should return 202 response when environment is deleted'() {
    given:
    def env = new Environment(name: ENV_NAME)
    environmentManager.findByName(_) >> env

    when:
    def result = resource.deleteEnvironmentByName(TEST_USER, ENV_NAME)

    then:
    expect result, notNullValue()
    expect result.status, equalTo(Response.Status.ACCEPTED.statusCode)
  }
}
