package io.cratekube.cloud.service

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.cratekube.cloud.AwsConfig
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.cratekube.cloud.api.TerraformApi
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.terraform.TerraformResource
import io.cratekube.cloud.model.terraform.TerraformState
import io.cratekube.cloud.modules.annotation.TerraformCmd
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config

import javax.inject.Inject

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require

/**
 * Default implementation of the {@link TerraformApi}.  Handles interactions with Terraform and the state it
 * maintains.
 */
@Slf4j
class TerraformService implements TerraformApi {
  FileSystemManager fs
  ObjectMapper objectMapper
  ProcessExecutor terraform
  TemplateProcessor handlebarsProcessor
  AwsConfig awsConfig

  @Inject
  TerraformService(FileSystemManager fs, ObjectMapper objectMapper, @TerraformCmd ProcessExecutor terraform,
                   TemplateProcessor handlebarsProcessor, @Config AwsConfig awsConfig) {
    this.fs = require fs, notNullValue()
    this.objectMapper = require objectMapper, notNullValue()
    this.terraform = require terraform, notNullValue()
    this.handlebarsProcessor = require handlebarsProcessor, notNullValue()
    this.awsConfig = require awsConfig, notNullValue()
  }

  @Override
  void init(FileObject directory) {
    require directory, notNullValue()
    require directory.folder, equalTo(true)

    // get all template files
    def templates = fs.resolveFile('res:terraform/templates').children.findAll { it.file }
    templates.each {
      def fileName = it.name.baseName.trim() - '.hbs'
      def parsedFile = handlebarsProcessor.parseFile("terraform/templates/${fileName}", [keypairName: awsConfig.keypair])
      def configFile = directory.resolveFile(fileName)
      // setup the file content and persist
      configFile.content.outputStream.withWriter { it.write(parsedFile) }
      configFile.createFile()
    }

    // place all of the regular files in the env directory
    def tfFiles = fs.resolveFile('res:terraform').children.findAll { it.file }
    tfFiles.each {
      def fileName = it.name.baseName
      def configFile = directory.resolveFile(fileName)
      // setup the file content and persist
      def content = it.content.inputStream.text
      configFile.content.outputStream.withWriter { it.write(content) }
      configFile.createFile()
    }

    // call terraform init
    def execDir = getEnvironmentDir(directory)
    terraform.exec(execDir, 'init').waitForProcessOutput(System.out, System.err)
  }

  @Override
  void apply(FileObject directory) {
    require directory, notNullValue()
    require directory.folder, equalTo(true)

    def execDir = getEnvironmentDir(directory)

    // plan, then apply the plan
    terraform.exec(execDir, 'plan', "-out=${Constants.ENV_PLAN}").waitForProcessOutput(System.out, System.err)
    terraform.exec(execDir, 'apply', Constants.ENV_PLAN).waitForProcessOutput(System.out, System.err)

    // cleanup the created plan file
    directory.resolveFile(Constants.ENV_PLAN).delete()
  }

  @Override
  void destroy(FileObject directory) {
    require directory, notNullValue()
    require directory.folder, equalTo(true)

    // call terraform destroy
    def execDir = getEnvironmentDir(directory)
    terraform.exec(execDir, 'destroy', '-auto-approve').waitForProcessOutput(System.out, System.err)

    // cleanup environment directory
    directory.deleteAll()
  }

  @Override
  List<TerraformResource> state(FileObject directory) {
    require directory, notNullValue()
    require directory.folder, equalTo(true)

    // execute terraform state to get the current resources for the directory
    def execDir = getEnvironmentDir(directory)
    def tfProc = terraform.exec(execDir, 'state', 'pull')
    def (out, err) = [new StringBuffer(), new StringBuffer()]
    tfProc.waitForProcessOutput(out, err)
    def tfState = objectMapper.readValue(out.toString(), TerraformState)

    return tfState.resources
  }

  private static File getEnvironmentDir(FileObject directory) {
    return new File(directory.name.path)
  }
}
