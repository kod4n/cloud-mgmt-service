package io.cratekube.cloud.service

import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentNotFoundException
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.Status
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.ExecutorService

import static io.cratekube.cloud.Fixtures.TEST_ENV
import static io.cratekube.cloud.Fixtures.envRequest
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.arrayContainingInAnyOrder
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class TerraformEnvironmentManagerSpec extends Specification {
  @Subject TerraformEnvironmentManager manager

  ProcessExecutor terraform
  ExecutorService executorService
  TemplateProcessor templateProcessor
  FileSystemManager fs
  ServiceConfig serviceConfig

  def setup() {
    terraform = Mock()
    executorService = Mock()
    templateProcessor = Mock()
    fs = Mock()
    executorService = Mock()
    serviceConfig = new ServiceConfig('aws', '/tmp/cloud-mgmt-config')
    manager = new TerraformEnvironmentManager(
      terraform, executorService, templateProcessor, fs, serviceConfig
    )
  }

  def 'should require valid constructor parameters'() {
    when:
    new TerraformEnvironmentManager(terraformProc, execService, tmplProcessor, fsm, svcConfig)

    then:
    thrown RequireViolation

    where:
    terraformProc  | execService          | tmplProcessor          | fsm     | svcConfig
    null           | null                 | null                   | null    | null
    this.terraform | null                 | null                   | null    | null
    this.terraform | this.executorService | null                   | null    | null
    this.terraform | this.executorService | this.templateProcessor | null    | null
    this.terraform | this.executorService | this.templateProcessor | this.fs | null
  }

  def 'should require valid parameters for create'() {
    when:
    manager.create(null)

    then:
    thrown RequireViolation
  }

  @PendingFeature
  def 'should throw exception when calling create for existing environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> true
    }

    when:
    manager.create(envRequest)

    then:
    thrown EnvironmentAlreadyExistsException
  }

  @PendingFeature
  def 'should return resources from terraform plan when creating environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }
    def tfState = getClass().getResource('/terraform/tf_state.json')

    when:
    def result = manager.create(envRequest)

    then:
    1 * terraform.exec(_, arrayContainingInAnyOrder('plan'))
    1 * terraform.exec(_, arrayContainingInAnyOrder('state')) >> tfState
    verifyAll(result) {
      expect it, notNullValue()
      expect name, equalTo(envRequest.name)
      expect provider, equalTo(serviceConfig.provider)
      expect resources, hasSize(2)
    }
  }

  @PendingFeature
  def 'should call appropriate api methods creating environment'() {
    given:
    def envRequest = envRequest()
    def envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${envRequest.name}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    manager.create(envRequest)

    then:
    _ * templateProcessor.parseFile(notEmptyString(), notNullValue())
    1 * terraform.exec(_, arrayContainingInAnyOrder('plan'))
    1 * terraform.exec(_, arrayContainingInAnyOrder('state'))
    1 * executorService.execute(notNullValue())
    1 * terraform.exec(_, arrayContainingInAnyOrder('apply'))
  }

  def 'should return empty list when filesystem cannot resolve environment directory'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    def result = manager.all

    then:
    expect result, notNullValue()
    expect result, empty()
  }

  //TODO
  @PendingFeature
  def 'should return all environments based on output from terraform state'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> true
      children >> [Mock(FileObject) { name >> 'foo' }, Mock(FileObject) { name >> 'bar' }].toArray()
    }

    def tfState = getClass().getResource('/terraform/tf_state.json').text
    terraform.exec(_, arrayContainingInAnyOrder('state')) >> GroovyMock(Process) {
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

  @PendingFeature
  def 'should return empty optional when environment directory is not found'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    def result = manager.findByName(TEST_ENV)

    then:
    expect result, notNullValue()
    expect result.present, equalTo(false)
  }

  @PendingFeature
  def 'should return environment data based on terraform state during findByName'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> true
    }

    def tfState = getClass().getResource('/terraform/tf_state.json').text
    terraform.exec(_, arrayContainingInAnyOrder('state')) >> GroovyMock(Process) {
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

  @PendingFeature
  def 'should require invoke terraform destroy during deleteByName'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> true
    }

    when:
    manager.deleteByName(TEST_ENV)

    then:
    1 * terraform.exec(arrayContainingInAnyOrder('destroy'))
  }

  @PendingFeature
  def 'should throw EnvironmentNotFound exception if env not found during deleteByName'() {
    given:
    String envDir = "${serviceConfig.configDir}/${Constants.ENV_CONFIG_PATH}/${TEST_ENV}"
    fs.resolveFile(envDir) >> Stub(FileObject) {
      exists() >> false
    }

    when:
    manager.deleteByName(TEST_ENV)

    then:
    thrown EnvironmentNotFoundException
  }
}
