package io.cratekube.cloud.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentNotFoundException
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.Status
import io.dropwizard.jackson.Jackson
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
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class TerraformEnvironmentManagerSpec extends Specification {
  @Subject
  TerraformEnvironmentManager manager

  ProcessExecutor terraform
  Executor executor
  TemplateProcessor templateProcessor
  FileSystemManager fs
  ObjectMapper objectMapper
  ServiceConfig serviceConfig

  def setup() {
    terraform = Mock()
    executor = Mock()
    templateProcessor = Mock()
    fs = Mock()
    executor = MoreExecutors.directExecutor()
    serviceConfig = new ServiceConfig('aws', '/tmp/cloud-mgmt-config', 'test-ssh-key')
    manager = new TerraformEnvironmentManager(
      terraform, executor, templateProcessor, fs, Jackson.newObjectMapper(), serviceConfig
    )
  }

  def 'should require valid constructor parameters'() {
    when:
    new TerraformEnvironmentManager(terraformProc, excutor, tmplProcessor, fsm, om, svcConfig)

    then:
    thrown RequireViolation

    where:
    terraformProc  | excutor       | tmplProcessor          | fsm     | om                | svcConfig
    null           | null          | null                   | null    | null              | null
    this.terraform | null          | null                   | null    | null              | null
    this.terraform | this.executor | null                   | null    | null              | null
    this.terraform | this.executor | this.templateProcessor | null    | null              | null
    this.terraform | this.executor | this.templateProcessor | this.fs | null              | null
    this.terraform | this.executor | this.templateProcessor | this.fs | this.objectMapper | null
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

  def 'should return resources from terraform plan when creating environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }
    fs.resolveFile('res:terraform/templates') >> Stub(FileObject) {
      getChildren() >> []
    }
    fs.resolveFile('res:terraform') >> Stub(FileObject) {
      getChildren() >> []
    }
    fs.resolveFile("${envDir}/env.plan") >> Stub(FileObject)

    when:
    def result = manager.create(envRequest)

    then:
    1 * terraform.exec(_, containsString('init'), *_) >> GroovyMock(Process)
    1 * terraform.exec(_, containsString('plan'), *_) >> GroovyMock(Process)
    1 * terraform.exec(_, containsString('apply'), *_) >> GroovyMock(Process)
    verifyAll(result) {
      expect it, notNullValue()
      expect name, equalTo(envRequest.name)
      expect provider, equalTo(serviceConfig.provider)
      expect status, equalTo(Status.PENDING)
    }
  }

  def 'should call appropriate api methods creating environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }
    fs.resolveFile('res:terraform/templates') >> Stub(FileObject) {
      getChildren() >> []
    }
    fs.resolveFile('res:terraform') >> Stub(FileObject) {
      getChildren() >> []
    }
    fs.resolveFile("${envDir}/env.plan") >> Stub(FileObject)

    when:
    manager.create(envRequest)

    then:
    _ * templateProcessor.parseFile(notEmptyString(), notNullValue())
    1 * terraform.exec(_, containsString('init'), *_) >> GroovyMock(Process)
    1 * terraform.exec(_, containsString('plan'), *_) >> GroovyMock(Process)
    1 * terraform.exec(_, containsString('apply'), *_) >> GroovyMock(Process)
  }

  def 'should return empty list when filesystem cannot resolve environment directory'() {
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

  def 'should return all environments based on output from terraform state'() {
    given:
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}"
    fs.resolveFile(envDir) >> Mock(FileObject) {
      exists() >> true
      getChildren() >> [fileObjectStub(baseName: 'foo'), fileObjectStub(baseName: 'bar')].toArray()
    }

    def tfState = getClass().getResource('/fixtures/tf_state.json').text
    terraform.exec(_, containsString('state'), *_) >> GroovyMock(Process) {
      waitForProcessOutput(_, _) >> { Appendable out, Appendable err ->
        out.append(tfState)
      }
    }

    when:
    def result = manager.all

    then:
    expect result, hasSize(2)
    expect result, hasItem(allOf(
      hasProperty('name', equalTo('foo')),
      hasProperty('provider', equalTo(serviceConfig.provider)),
      hasProperty('resources', hasSize(2))
    ))
    expect result, hasItem(allOf(
      hasProperty('name', equalTo('bar')),
      hasProperty('provider', equalTo(serviceConfig.provider)),
      hasProperty('resources', hasSize(2))
    ))
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
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> fileObjectStub(baseName: TEST_ENV)

    def tfState = getClass().getResource('/fixtures/tf_state.json').text
    terraform.exec(_, containsString('state'), *_) >> GroovyMock(Process) {
      waitForProcessOutput(_, _) >> { Appendable out, Appendable err ->
        out.append(tfState)
      }
    }

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
        hasProperty('status', equalTo(Status.APPLIED)),
      ))
      expect resources, hasItem(allOf(
        hasProperty('name', equalTo('bar')),
        hasProperty('status', equalTo(Status.APPLIED)),
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
    String envDir = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> fileObjectStub(baseName: TEST_ENV)

    when:
    manager.deleteByName(TEST_ENV)

    then:
    1 * terraform.exec(_, containsString('destroy'), *_) >> GroovyMock(Process)
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
