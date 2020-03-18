package io.cratekube.cloud.resources

import io.cratekube.cloud.BaseIntegrationSpec
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.model.Environment
import spock.lang.PendingFeature

import javax.inject.Inject
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response

import static io.cratekube.cloud.Fixtures.TEST_ENV
import static javax.ws.rs.client.Entity.json
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class EnvironmentResourceIntegrationSpec extends BaseIntegrationSpec {
  String baseRequestPath = '/environment'
  List<Object> requiredMocks = [environmentManager]

  @Inject EnvironmentManager environmentManager

  @PendingFeature
  def 'should get list response when executing GET'() {
    given:
    environmentManager.all >> [new Environment(id: UUID.randomUUID(), name: TEST_ENV)]

    when:
    def result = baseRequest().get(new GenericType<List<Environment>>() {})

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect it, hasSize(1)
      expect it.first(), hasProperty('name', equalTo(TEST_ENV))
    }
  }

  def 'should return empty list when manager return no results when executing GET'() {
    given:
    environmentManager.all >> []

    when:
    def result = baseRequest().get(new GenericType<List<Environment>>() {})

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect it, hasSize(0)
    }
  }

  @PendingFeature
  def 'should return accepted response when creating an environment'() {
    given:
    environmentManager.create(notNullValue()) >> new Environment(id: UUID.randomUUID(), name: TEST_ENV)

    when:
    def result = baseRequest().post(json(new EnvironmentResource.EnvironmentRequest(TEST_ENV)))

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect status, equalTo(Response.Status.ACCEPTED)
    }
  }

  @PendingFeature
  def 'should return environment when found by manager'() {
    given:
    environmentManager.findByName(TEST_ENV) >> Optional.of(new Environment(id: UUID.randomUUID(), name: TEST_ENV))

    when:
    def result = baseRequest("/${TEST_ENV}").get()
    def env = result.readEntity(Environment)

    then:
    verifyAll(result) {
      expect status, equalTo(Response.Status.OK)
      expect env.id, notNullValue(UUID)
      expect env.name, equalTo(TEST_ENV)
    }
  }

  @PendingFeature
  def 'should return not found response when environment cannot be found by manager'() {
    given:
    environmentManager.findByName(TEST_ENV) >> Optional.empty()

    when:
    def result = baseRequest("/${TEST_ENV}").get()

    then:
    expect result.status, equalTo(Response.Status.NOT_FOUND)
  }

  @PendingFeature
  def 'should 204 no content response when no exception occurs on delete'() {
    when:
    def result = baseRequest("/${TEST_ENV}").delete()

    then:
    1 * environmentManager.deleteByName(TEST_ENV)
    expect result.status, equalTo(Response.Status.ACCEPTED)
  }
}
