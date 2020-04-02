package io.cratekube.cloud.service

import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.dropwizard.jackson.Jackson
import org.apache.commons.vfs2.FileName
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static spock.util.matcher.HamcrestSupport.expect

class TerraformServiceSpec extends Specification {
  @Subject TerraformService subject

  FileSystemManager fs
  ProcessExecutor terraform
  TemplateProcessor templateProcessor
  ServiceConfig serviceConfig

  def setup() {
    fs = Mock()
    terraform = Mock()
    templateProcessor = Mock()
    serviceConfig = new ServiceConfig('aws', '/tmp/cloud-mgmt-config', 'test-ssh-key')
    subject = new TerraformService(fs, Jackson.newObjectMapper(), terraform, templateProcessor, serviceConfig)
  }

  def 'should require valid constructor params'() {
    when:
    new TerraformService(fsm, om, terraformProc, tmplProcessor, svcConfig)

    then:
    thrown RequireViolation

    where:
    fsm     | om                        | terraformProc  | tmplProcessor          | svcConfig
    null    | null                      | null           | null                   | null
    this.fs | null                      | null           | null                   | null
    this.fs | Jackson.newObjectMapper() | null           | null                   | null
    this.fs | Jackson.newObjectMapper() | this.terraform | null                   | null
    this.fs | Jackson.newObjectMapper() | this.terraform | this.templateProcessor | null
  }

  def 'should require valid params for init'() {
    when:
    subject.init(dir)

    then:
    thrown RequireViolation

    where:
    dir << [null, Stub(FileObject) { isFolder() >> false }]
  }

  def 'should call correct apis during init'() {
    given:
    def envDir = Stub(FileObject) {
      isFolder() >> true
      resolveFile('env.plan') >> Stub(FileObject)
    }
    fs.resolveFile('res:terraform/templates') >> Stub(FileObject) {
      getChildren() >> []
    }
    fs.resolveFile('res:terraform') >> Stub(FileObject) {
      getChildren() >> []
    }

    when:
    subject.init(envDir)

    then:
    1 * terraform.exec(_, containsString('init'), *_) >> GroovyMock(Process)
  }

  def 'should require valid params for apply'() {
    when:
    subject.apply(dir)

    then:
    thrown RequireViolation

    where:
    dir << [null, Stub(FileObject) { isFolder() >> false }]
  }

  def 'should call correct apis during apply'() {
    given:
    def planObject = Mock(FileObject)
    def envDir = Stub(FileObject) {
      isFolder() >> true
      resolveFile('env.plan') >> planObject
    }

    when:
    subject.apply(envDir)

    then:
    1 * terraform.exec(_, containsString('plan'), *_) >> GroovyMock(Process)
    1 * terraform.exec(_, containsString('apply'), *_) >> GroovyMock(Process)
    1 * planObject.delete()
  }

  def 'should require valid params for destroy'() {
    when:
    subject.destroy(dir)

    then:
    thrown RequireViolation

    where:
    dir << [null, Stub(FileObject) { isFolder() >> false }]
  }

  def 'should call correct apis during destroy'() {
    given:
    def envDir = Mock(FileObject) {
      isFolder() >> true
      getName() >> Stub(FileName)
    }

    when:
    subject.destroy(envDir)

    then:
    1 * terraform.exec(_, containsString('destroy'), *_) >> GroovyMock(Process)
    1 * envDir.deleteAll()
  }

  def 'should require valid params for state'() {
    when:
    subject.state(dir)

    then:
    thrown RequireViolation

    where:
    dir << [null, Stub(FileObject) { isFolder() >> false }]
  }

  def 'should return terraform resources based on output from terraform state'() {
    given:
    def envDir = Mock(FileObject) {
      isFolder() >> true
      getName() >> Stub(FileName)
    }

    def tfState = getClass().getResource('/fixtures/tf_state.json').text
    terraform.exec(_, containsString('state'), *_) >> GroovyMock(Process) {
      waitForProcessOutput(_, _) >> { Appendable out, Appendable err ->
        out.append(tfState)
      }
    }

    when:
    def result = subject.state(envDir)

    then:
    expect result, hasSize(2)
    expect result, hasItem(allOf(
      hasProperty('name', equalTo('foo')),
      hasProperty('provider', equalTo('provider.aws')),
      hasProperty('instances', hasSize(1))
    ))
    expect result, hasItem(allOf(
      hasProperty('name', equalTo('bar')),
      hasProperty('provider', equalTo('provider.aws')),
      hasProperty('instances', hasSize(1))
    ))
  }
}
