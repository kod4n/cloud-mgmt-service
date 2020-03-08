package io.cratekube.cloud.service

import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.resources.EnvironmentResource
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.ExecutorService

import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class TerraformEnvironmentManagerSpec extends Specification {
  @Subject TerraformEnvironmentManager manager

  ProcessExecutor terraform
  ExecutorService executorService

  def setup() {
    terraform = Mock(ProcessExecutor)
    executorService = Mock(ExecutorService)
    manager = new TerraformEnvironmentManager(terraform, executorService, 'aws')
  }

  def 'should require valid constructor parameters'() {
    when:
    new TerraformEnvironmentManager(terraformProc, execService, cloudProvider)

    then:
    thrown RequireViolation

    where:
    terraformProc  | execService          | cloudProvider
    null           | null                 | null
    this.terraform | null                 | null
    this.terraform | this.executorService | null
    this.terraform | this.executorService | ''
  }

  def 'should require valid parameters for create'() {
    when:
    manager.create(null)

    then:
    thrown RequireViolation
  }

  @PendingFeature
  def 'should require invoke terraform during create'() {
    when:
    manager.create(new EnvironmentResource.EnvironmentRequest('test-env'))

    then:
    1 * terraform.exec('apply')
  }

  def 'should return non-null list on getAll'() {
    when:
    def result = manager.all

    then:
    expect result, notNullValue()
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
  def 'should require invoke terraform state during findByName'() {
    when:
    manager.findByName('test-env')

    then:
    1 * terraform.exec('state')
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
    when:
    manager.deleteByName('test-env')

    then:
    1 * terraform.exec('destroy')
  }
}
