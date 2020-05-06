package io.cratekube.cloud.service

import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentNotFoundException
import io.cratekube.cloud.api.EnvironmentOperationPendingException
import io.cratekube.cloud.api.TerraformApi
import io.cratekube.cloud.api.TerraformStateConverter
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.Environment
import io.cratekube.cloud.model.Status
import io.cratekube.cloud.model.terraform.TerraformInstance
import io.cratekube.cloud.model.terraform.TerraformResource
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Executor

import static io.cratekube.cloud.Fixtures.TEST_ENV
import static io.cratekube.cloud.Fixtures.envRequest
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class TerraformEnvironmentManagerSpec extends Specification {
  @Subject
  TerraformEnvironmentManager manager

  Executor executor
  TerraformApi terraform
  TerraformStateConverter terraformStateConverter
  FileSystemManager fs
  ServiceConfig serviceConfig
  Map<String, Environment> environmentCache

  def setup() {
    executor = MoreExecutors.directExecutor()
    terraform = Mock()
    terraformStateConverter = new DefaultTerraformStateConverter()
    fs = Mock()
    serviceConfig = new ServiceConfig('aws', '/tmp/cloud-mgmt-config', 'test-ssh-key')
    environmentCache = [:]
    manager = new TerraformEnvironmentManager(executor, terraform, terraformStateConverter, fs, serviceConfig, environmentCache)
  }

  def 'should require valid constructor parameters'() {
    when:
    new TerraformEnvironmentManager(excutor, terraformApi, terraformState, fsm, svcConfig, envCache)

    then:
    thrown RequireViolation

    where:
    excutor       | terraformApi   | terraformState               | fsm     | svcConfig          | envCache
    null          | null           | null                         | null    | null               | null
    this.executor | null           | null                         | null    | null               | null
    this.executor | this.terraform | null                         | null    | null               | null
    this.executor | this.terraform | this.terraformStateConverter | null    | null               | null
    this.executor | this.terraform | this.terraformStateConverter | this.fs | null               | null
    this.executor | this.terraform | this.terraformStateConverter | this.fs | this.serviceConfig | null
  }

  def 'should require valid parameters for create'() {
    when:
    manager.create(null)

    then:
    thrown RequireViolation
  }

  def 'should throw exception when calling create for existing environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> true
    }

    when:
    manager.create(envRequest)

    then:
    thrown EnvironmentAlreadyExistsException
  }

  def 'should throw exception when calling create for pending environment'() {
    given:
    environmentCache[TEST_ENV] = new Environment(status: Status.PENDING)

    when:
    manager.create(envRequest())

    then:
    thrown EnvironmentOperationPendingException
  }

  def 'should return resources from terraform state when creating environment'() {
    given:
    def envRequest = envRequest()

    // must not exist initially, but must have basename for subsequent calls
    def envDir = fileObjectStub(exists: false, baseName: TEST_ENV)
    fs.resolveFile(_) >> envDir

    // mocked for cache population done in executor
    terraform.state(_) >> [
      new TerraformResource(name: 'foo', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'foo_instance_1', tags: [Name: 'foo'], public_dns: 'dns.value.1', public_ip: 'x.x.x.x'])
      ]),
      new TerraformResource(name: 'bar', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'bar_instance_1', tags: [Name: 'bar'], public_dns: 'dns.value.2', public_ip: 'y.y.y.y'])
      ])
    ]

    when:
    def result = manager.create(envRequest)

    then:
    expect environmentCache, hasKey(TEST_ENV)
    expect environmentCache[TEST_ENV], hasProperty('status', equalTo(Status.APPLIED))
    verifyAll(result) {
      expect it, notNullValue()
      expect name, equalTo(TEST_ENV)
      expect provider, equalTo(serviceConfig.provider)
      expect resources, hasSize(2)
      expect resources, hasItem(allOf(
        hasProperty('id', equalTo('foo_instance_1')),
        hasProperty('name', equalTo('foo')),
        hasProperty('type', equalTo('aws_instance'))
      ))
      expect resources, hasItem(allOf(
        hasProperty('id', equalTo('bar_instance_1')),
        hasProperty('name', equalTo('bar')),
        hasProperty('type', equalTo('aws_instance'))
      ))
    }
  }

  def 'should call appropriate api methods creating environment'() {
    given:
    def envRequest = envRequest()
    def envDir = Mock(FileObject) {
      exists() >> false
      getName() >> Mock(FileName) {
        getBaseName() >> TEST_ENV
      }
    }
    fs.resolveFile(_) >> envDir

    // mocked for cache population done in executor
    def tfState = [
      new TerraformResource(name: 'foo', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'foo_instance_1', tags: [Name: 'foo'], public_dns: 'dns.value.1', public_ip: 'x.x.x.x'])
      ]),
      new TerraformResource(name: 'bar', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'bar_instance_1', tags: [Name: 'bar'], public_dns: 'dns.value.2', public_ip: 'y.y.y.y'])
      ])
    ]

    when:
    manager.create(envRequest)

    then:
    1 * envDir.createFolder()
    1 * terraform.init(envDir)
    1 * terraform.apply(envDir)
    1 * terraform.state(envDir) >> tfState
  }

  def 'should return empty list when filesystem cannot resolve environment directory and cache is empty'() {
    given:
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    def result = manager.all

    then:
    expect result, notNullValue()
    expect result, empty()
  }

  def 'should return all environments based on output from terraform state converter'() {
    given:
    def envDir = Mock(FileObject) {
      exists() >> true
      getChildren() >> [fileObjectStub(baseName: 'test-env')].toArray()
    }
    fs.resolveFile(_) >> envDir

    terraform.state(envDir) >> [
      new TerraformResource(name: 'foo', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'foo_instance_1', tags: [Name: 'foo'], public_dns: 'dns.value.1', public_ip: 'x.x.x.x'])
      ]),
      new TerraformResource(name: 'bar', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'bar_instance_1', tags: [Name: 'bar'], public_dns: 'dns.value.2', public_ip: 'y.y.y.y'])
      ])
    ]

    when:
    def result = manager.all
    def env = result.first()

    then:
    verifyAll(env) {
      expect name, equalTo('test-env')
      expect provider, equalTo(serviceConfig.provider)
      expect resources, hasSize(2)
      expect resources, hasItem(allOf(
        hasProperty('id', equalTo('foo_instance_1')),
        hasProperty('name', equalTo('foo')),
        hasProperty('type', equalTo('aws_instance'))
      ))
      expect resources, hasItem(allOf(
        hasProperty('id', equalTo('bar_instance_1')),
        hasProperty('name', equalTo('bar')),
        hasProperty('type', equalTo('aws_instance'))
      ))
    }
  }

  def 'should require valid parameters for findByName'() {
    when:
    manager.findByName(name)

    then:
    thrown RequireViolation

    where:
    name << [null, '']
  }

  def 'should return empty optional when environment directory is not found'() {
    given:
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    def result = manager.findByName(TEST_ENV)

    then:
    expect result, notNullValue()
    expect result.present, equalTo(false)
  }

  def 'should return environment data based on terraform state during findByName'() {
    given:
    fs.resolveFile(_) >> fileObjectStub(baseName: TEST_ENV)

    terraform.state(_) >> [
      new TerraformResource(name: 'foo', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'foo_instance_1', tags: [Name: 'foo'], public_dns: 'dns.value.1', public_ip: 'x.x.x.x'])
      ]),
      new TerraformResource(name: 'bar', provider: 'provider.aws', type: 'aws_instance', instances: [
        new TerraformInstance(attributes: [id: 'bar_instance_1', tags: [Name: 'bar'], public_dns: 'dns.value.2', public_ip: 'y.y.y.y'])
      ])
    ]

    when:
    def result = manager.findByName(TEST_ENV)

    then:
    expect result.present, equalTo(true)
    verifyAll(result.get()) {
      expect name, equalTo(TEST_ENV)
      expect provider, equalTo(serviceConfig.provider)
      expect resources, hasSize(2)
      expect resources, hasItem(allOf(
        hasProperty('name', equalTo('foo')),
        hasProperty('id', equalTo('foo_instance_1')),
        hasProperty('type', equalTo('aws_instance'))
      ))
      expect resources, hasItem(allOf(
        hasProperty('name', equalTo('bar')),
        hasProperty('id', equalTo('bar_instance_1')),
        hasProperty('type', equalTo('aws_instance'))
      ))
    }
  }

  def 'should require valid parameters for deleteByName'() {
    when:
    manager.deleteByName(name)

    then:
    thrown RequireViolation

    where:
    name << [null, '']
  }

  def 'should require invoke terraform destroy during deleteByName'() {
    given:
    def envDir = Mock(FileObject) {
      exists() >> true
      getChildren() >> [fileObjectStub(baseName: 'test-env')].toArray()
    }
    fs.resolveFile(_) >> envDir

    when:
    manager.deleteByName(TEST_ENV)

    then:
    1 * terraform.destroy(envDir)
  }

  def 'should throw EnvironmentNotFound exception if env not found during deleteByName'() {
    given:
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> fileObjectStub(exists: false, baseName: TEST_ENV)

    when:
    manager.deleteByName(TEST_ENV)

    then:
    thrown EnvironmentNotFoundException
  }

  def 'should throw exception when calling deleteByName for pending environment'() {
    given:
    environmentCache[TEST_ENV] = new Environment(status: Status.PENDING)

    when:
    manager.deleteByName(TEST_ENV)

    then:
    thrown EnvironmentOperationPendingException
  }

  // Helper methods for common stubs and mocks //

  FileObject fileObjectStub(Map opts) {
    return Stub(FileObject) {
      exists() >> opts.getOrDefault('exists', true)
      getName() >> Stub(FileName) {
        getBaseName() >> opts.getOrDefault('baseName', 'test-file')
      }
    }
  }
}
