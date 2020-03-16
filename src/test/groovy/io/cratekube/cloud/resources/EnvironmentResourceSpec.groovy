package io.cratekube.cloud.resources

import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.auth.User
import io.cratekube.cloud.model.Environment
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.core.Response

import static org.hamcrest.Matchers.equalTo
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

  def 'should verify example response'() {
    when:
    def result = resource.environments

    then:
    expect result, notNullValue()
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

  @PendingFeature
  def 'should return environment from manager for getEnvironmentById'() {
    given:
    def env = new Environment(name: ENV_NAME)
    environmentManager.findByName(_) >> env

    when:
    def result = resource.getEnvironmentByName(TEST_USER, ENV_NAME)

    then:
    expect result, notNullValue()
    expect result.name, equalTo(env.name)
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

  @PendingFeature
  def 'should return environment from manager for createEnvironment'() {
    given:
    def env = new Environment(id: UUID.randomUUID(), name: ENV_NAME)
    environmentManager.findByName(_) >> env

    when:
    def result = resource.createEnvironment(TEST_USER, new EnvironmentResource.EnvironmentRequest(ENV_NAME))

    then:
    expect result, notNullValue()
    expect result.name, equalTo(env.name)
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

  @PendingFeature
  def 'should return 201 response when environment is deleted'() {
    given:
    def env = new Environment(id: UUID.randomUUID(), name: ENV_NAME)
    environmentManager.findByName(_) >> env

    when:
    def result = resource.deleteEnvironmentByName(TEST_USER, ENV_NAME)

    then:
    expect result, notNullValue()
    expect result.status, equalTo(Response.Status.ACCEPTED)
  }
}
