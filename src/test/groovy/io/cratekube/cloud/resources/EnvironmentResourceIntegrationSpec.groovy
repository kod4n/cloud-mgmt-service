package io.cratekube.cloud.resources

import io.cratekube.cloud.BaseIntegrationSpec
import io.cratekube.cloud.model.Environment

import javax.ws.rs.core.GenericType

import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class EnvironmentResourceIntegrationSpec extends BaseIntegrationSpec {
  String baseRequestPath = '/environment'

  def 'should get list response when executing GET'() {
    when:
    def result = baseRequest().get(new GenericType<List<Environment>>() {})

    then:
    expect result, notNullValue()
  }
}
