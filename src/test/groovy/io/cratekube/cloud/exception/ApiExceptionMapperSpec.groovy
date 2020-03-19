package io.cratekube.cloud.exception

import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentNotFoundException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ApiExceptionMapperSpec extends Specification {
  @Subject ApiExceptionMapper subject

  def setup() {
    subject = new ApiExceptionMapper()
  }

  @Unroll
  def 'should return #expectedCode for exception #exception'() {
    when:
    def result = subject.toResponse(exception)

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect status, equalTo(expectedCode)
    }

    where:
    exception                                                   | expectedCode
    new EnvironmentNotFoundException('env not found')           | 404
    new EnvironmentAlreadyExistsException('env already exists') | 400
  }
}
