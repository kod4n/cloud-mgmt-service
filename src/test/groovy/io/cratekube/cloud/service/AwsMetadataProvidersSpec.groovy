package io.cratekube.cloud.service

import io.cratekube.cloud.api.ResourceMetadataProvider
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class AwsMetadataProvidersSpec extends Specification {
  def 'ec2 metadata provider should return correct object'() {
    given:
    ResourceMetadataProvider provider = AwsMetadataProviders.EC2_METADATA_PROVIDER
    def attributes = [public_dns: 'dns.value', public_ip: 'x.x.x.x']

    when:
    def result = provider.getMetadata(attributes)

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect publicDns, equalTo('dns.value')
      expect publicIp, equalTo('x.x.x.x')
    }
  }

  def 'default metadata provider should return correct base provider'() {
    given:
    ResourceMetadataProvider provider = AwsMetadataProviders.DEFAULT_METADATA_PROVIDER
    def attributes = [public_dns: 'dns.value', public_ip: 'x.x.x.x']

    when:
    def result = provider.getMetadata(attributes)

    then:
    expect result, notNullValue()
    expect result.isEmpty(), equalTo(true)
  }
}
