package io.cratekube.cloud.service

import com.github.jknack.handlebars.Handlebars
import groovy.util.logging.Slf4j
import io.cratekube.cloud.api.TemplateProcessor

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Template processor implementation using handlebars.
 */
@Slf4j
class HandlebarsTemplateProcessor implements TemplateProcessor {
  Handlebars handlebars

  @Inject
  HandlebarsTemplateProcessor(Handlebars handlebars) {
    this.handlebars = require handlebars, notNullValue()
  }

  @Override
  String parseFile(String filePath, Object data) {
    require filePath, notEmptyString()
    require data, notNullValue()

    // compile the template from the filePath
    log.debug 'parsing template file from resource [{}]', filePath
    def template = handlebars.compile(filePath)

    // parse the template using the data object
    def parsed = template.apply(data)
    log.trace 'parsed resource [{}] with output:\n{}', filePath, parsed

    return parsed
  }
}
