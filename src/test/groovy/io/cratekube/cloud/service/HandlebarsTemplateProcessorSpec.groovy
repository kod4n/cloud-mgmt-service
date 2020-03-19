package io.cratekube.cloud.service

import com.github.jknack.handlebars.Handlebars
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class HandlebarsTemplateProcessorSpec extends Specification {
  @Subject HandlebarsTemplateProcessor subject

  def setup() {
    subject = new HandlebarsTemplateProcessor(new Handlebars())
  }

  def 'should require valid constructor parameters'() {
    when:
    new HandlebarsTemplateProcessor(null)

    then:
    thrown RequireViolation
  }

  @Unroll
  def 'should thrown require validation when calling parse(#filePath, #data)'() {
    when:
    subject.parseFile(filePath, data)

    then:
    thrown RequireViolation

    where:
    filePath    | data
    null        | null
    ''          | null
    'some/path' | null
  }

  @PendingFeature
  def 'should return parsed template when calling parse'() {
    given:
    def filePath = 'templates/test'
    def data = [name: 'spock-test']

    when:
    def result = subject.parseFile(filePath, data)

    then:
    expect result, notEmptyString()
    expect result.trim(), equalTo("testing template ${data.name}" as String)
  }
}
