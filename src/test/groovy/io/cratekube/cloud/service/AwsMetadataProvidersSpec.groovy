package io.cratekube.cloud.service

import io.cratekube.cloud.api.ResourceMetadataProvider
import io.cratekube.cloud.model.ResourceMetadata
import io.cratekube.cloud.model.aws.Ec2Metadata
import spock.lang.Specification
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue
import static spock.util.matcher.HamcrestSupport.expect

class AwsMetadataProvidersSpec extends Specification {
  def 'ec2 metadata provider should return correct object'() {
    given:
    ResourceMetadataProvider<Ec2Metadata> provider = AwsMetadataProviders.ec2MetadataProvider
    def attributes = [public_dns: 'dns.value', public_ip: 'x.x.x.x']

    when:
    def result = provider.getMetadata(attributes)

    then:
    verifyAll(result) {
      expect it, notNullValue()
      expect it.getClass(), equalTo(Ec2Metadata)
      expect publicDns, equalTo('dns.value')
      expect publicIp, equalTo('x.x.x.x')
    }
  }

  def 'default metadata provider should return correct base provider'() {
    given:
    ResourceMetadataProvider<ResourceMetadata> provider = AwsMetadataProviders.defaultMetadataProvider
    def attributes = [public_dns: 'dns.value', public_ip: 'x.x.x.x']

    when:
    def result = provider.getMetadata(attributes)

    then:
    expect result, nullValue()
  }

  @Unroll
  def 'getMetadataProvider(#type) should return #objectClass provider'() {
    when:
    def result = AwsMetadataProviders.getMetadataProvider(type)
    def metadata = result.getMetadata([:])

    then:
    expect metadata.class, equalTo(objectClass)

    where:
    type           | objectClass
    'aws_instance' | Ec2Metadata
  }
}
