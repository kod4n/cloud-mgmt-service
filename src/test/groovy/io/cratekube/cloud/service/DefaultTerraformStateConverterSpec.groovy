package io.cratekube.cloud.service

import io.cratekube.cloud.model.Status
import io.cratekube.cloud.model.terraform.TerraformInstance
import io.cratekube.cloud.model.terraform.TerraformResource
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class DefaultTerraformStateConverterSpec extends Specification {
  @Subject DefaultTerraformStateConverter subject

  def setup() {
    subject = new DefaultTerraformStateConverter()
  }

  def 'should throw exception when convert is called with null list'() {
    when:
    subject.convert(null)

    then:
    thrown RequireViolation
  }

  def 'should return empty list when providing empty list'() {
    when:
    def result = subject.convert([])

    then:
    expect result, empty()
  }

  def 'should return populated list of managed resources when converting'() {
    given:
    def tfResources = [
      new TerraformResource(
        type: 'aws_instance',
        name: 'instance_name',
        provider: 'provider.aws',
        instances: [
          new TerraformInstance(attributes: [
            id: 'test_id',
            tags: [Name: 'tag_name'],
            public_dns: 'dns.value',
            public_ip: 'x.x.x.x'
          ])
        ]
      )
    ]

    when:
    def result = subject.convert(tfResources)

    then:
    verifyAll(result.first()) {
      expect id, equalTo('test_id')
      expect name, equalTo('instance_name')
      expect type, equalTo('aws_instance')
      expect status, equalTo(Status.APPLIED)
      expect metadata, allOf(
        hasEntry(equalTo('publicDns'), notEmptyString()),
        hasEntry(equalTo('publicIp'), notEmptyString())
      )
    }
  }

  def 'should return list of managed resources using tag name when resource has multiple instances'() {
    given:
    def tfResources = [
      new TerraformResource(
        type: 'aws_instance',
        name: 'instance_name',
        provider: 'provider.aws',
        instances: [
          new TerraformInstance(attributes: [
            id: 'test_id_1',
            tags: [Name: 'instance-1'],
            public_dns: 'dns.value',
            public_ip: 'x.x.x.x'
          ]),
          new TerraformInstance(attributes: [
            id: 'test_id_2',
            tags: [Name: 'instance-2'],
            public_dns: 'dns.value.2',
            public_ip: 'y.y.y.y'
          ])
        ]
      )
    ]

    when:
    def result = subject.convert(tfResources)

    then:
    verifyAll(result[0]) {
      expect id, equalTo('test_id_1')
      expect name, equalTo('instance-1')
      expect type, equalTo('aws_instance')
      expect status, equalTo(Status.APPLIED)
      expect metadata, allOf(
        hasEntry(equalTo('publicDns'), notEmptyString()),
        hasEntry(equalTo('publicIp'), notEmptyString())
      )
    }
    verifyAll(result[1]) {
      expect id, equalTo('test_id_2')
      expect name, equalTo('instance-2')
      expect type, equalTo('aws_instance')
      expect status, equalTo(Status.APPLIED)
      expect metadata, allOf(
        hasEntry(equalTo('publicDns'), notEmptyString()),
        hasEntry(equalTo('publicIp'), notEmptyString())
      )
    }
  }
}
