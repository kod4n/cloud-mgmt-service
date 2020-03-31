package io.cratekube.cloud.service

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.api.EnvironmentNotFoundException
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.Environment
import io.cratekube.cloud.model.ManagedResource
import io.cratekube.cloud.model.Status
import io.cratekube.cloud.modules.annotation.TerraformCmd
import io.cratekube.cloud.resources.EnvironmentResource.EnvironmentRequest
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config

import javax.inject.Inject
import java.util.concurrent.Executor

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation for the {@link EnvironmentManager}.
 */
@Slf4j
class TerraformEnvironmentManager implements EnvironmentManager {
  ProcessExecutor terraform
  Executor executor
  TemplateProcessor handlebarsProcessor
  FileSystemManager fs
  ObjectMapper objectMapper
  ServiceConfig serviceConfig

  @Inject
  TerraformEnvironmentManager(@TerraformCmd ProcessExecutor terraform, Executor executor,
                              TemplateProcessor handlebarsProcessor, FileSystemManager fs,
                              ObjectMapper objectMapper, @Config ServiceConfig serviceConfig) {
    this.terraform = require terraform, notNullValue()
    this.executor = require executor, notNullValue()
    this.handlebarsProcessor = require handlebarsProcessor, notNullValue()
    this.fs = require fs, notNullValue()
    this.objectMapper = require objectMapper, notNullValue()
    this.serviceConfig = require serviceConfig, notNullValue()
  }

  @Override
  Environment create(EnvironmentRequest environmentRequest) throws EnvironmentAlreadyExistsException {
    require environmentRequest, notNullValue()

    def environmentName = environmentRequest.name

    // check to see if an environment directory already exists for this env
    def envPath = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${environmentName}"
    def envDir = resolveEnvDirectory(environmentName)

    // throw exception if the environments dir is found
    if (envDir.exists()) {
      def exception = new EnvironmentAlreadyExistsException("environment ${environmentName} already exists")
      log.debug 'environment [{}] already exists, cannot create', environmentName, exception
      throw exception
    }

    // create the environment directory
    envDir.createFolder()

    // parse template files, initialize terraform
    initializeEnvDirectory(environmentName)

    // start the environment creation in the executor service
    executor.execute {
      def envFile = new File("${envDir.name.path}")

      terraform.with {
        // init and plan
        exec(envFile, 'init').waitForProcessOutput(System.out, System.err)
        exec(envFile, 'plan', "-out=${Constants.ENV_PLAN}").waitForProcessOutput(System.out, System.err)
        // apply the plan
        exec(envFile, 'apply', Constants.ENV_PLAN).waitForProcessOutput(System.out, System.err)
      }

      // cleanup the plan file
      fs.resolveFile("${envPath}/${Constants.ENV_PLAN}").delete()
    }

    return new Environment(name: environmentName, provider: serviceConfig.provider, status: Status.PENDING)
  }

  @Override
  List<Environment> getAll() {
    // find all environments provisioned via terraform state
    def envsDir = resolveEnvDirectory()

    // return empty list if the environments dir is not found or has no children directories
    if (!envsDir.exists() || !envsDir?.children) {
      log.debug 'no environments found, returning empty list'
      return []
    }

    // iterate through each env directory and get the current terraform state for each env
    return envsDir.children.collect { envDir -> pullEnvironmentState(envDir) }
  }

  @Override
  Optional<Environment> findByName(String environmentName) {
    require environmentName, notEmptyString()

    // find all environments provisioned via terraform state
    def envDir = resolveEnvDirectory(environmentName)

    // return empty list if the environments dir is not found or has no children directories
    if (!envDir.exists()) {
      log.debug 'environment [{}] not found, returning empty result', environmentName
      return Optional.empty()
    }

    return Optional.of(pullEnvironmentState(envDir))
  }

  private Environment pullEnvironmentState(FileObject envDir) {
    require envDir, notNullValue()

    def envName = envDir.name.baseName
    log.debug 'pulling terraform state for environment [{}]', envName

    // execute terraform state to get the current resources for the env
    def tfProc = terraform.exec(new File(envDir.name.path), 'state', 'pull')
    def (out, err) = [new StringBuffer(), new StringBuffer()]
    tfProc.waitForProcessOutput(out, err)
    def tfState = objectMapper.readValue(out.toString(), Map)

    return new Environment(
      name: envName,
      provider: serviceConfig.provider,
      resources: (tfState.resources as List<Map>).collect {
        new ManagedResource(name: it.name, status: Status.APPLIED)
      }
    )
  }

  @Override
  void deleteByName(String environmentName) throws EnvironmentNotFoundException {
    require environmentName, notEmptyString()

    // find all environments provisioned via terraform state
    def envPath = "${serviceConfig.configDir}${Constants.ENV_CONFIG_PATH}/${environmentName}"
    log.debug 'checking for environment data at directory [{}]', envPath
    def envDir = resolveEnvDirectory(environmentName)

    // throw exception if the environments dir is not found
    if (!envDir.exists()) {
      def exception = new EnvironmentNotFoundException("environment ${environmentName} not found")
      log.debug 'environment [{}] not found, cannot delete', environmentName, exception
      throw exception
    }

    // start environment deletion using executor
    executor.execute {
      // if environment exists run terraform destroy
      log.debug 'destroying terraform resources for environment [{}]', environmentName
      def tfProc = terraform.exec(new File(envDir.name.path), 'destroy', '-auto-approve')
      tfProc.waitForProcessOutput(System.out, System.err)

      // cleanup the environment directory after the destroy process finishes
      log.debug 'deleting environment directory and config files at directory [{}]', envPath
      envDir.deleteAll()
    }
  }

  private FileObject resolveEnvDirectory(String environmentName = '') {
    def dirParts = [serviceConfig.configDir, Constants.ENVS_DIR]
    if (environmentName) {
      dirParts << environmentName
    }

    def dirPath = dirParts.join(File.separator)

    log.debug 'resolving environment data directory [{}]', dirPath
    return fs.resolveFile(dirPath)
  }

  private void initializeEnvDirectory(String environmentName) {
    require environmentName, notEmptyString()

    def envPath = [serviceConfig.configDir, Constants.ENVS_DIR, environmentName].join(File.separator)

    // get all template files
    def templates = fs.resolveFile('res:terraform/templates').children.findAll { it.file }
    templates.each {
      def fileName = it.name.baseName.trim() - '.hbs'
      def parsedFile = handlebarsProcessor.parseFile("terraform/templates/${fileName}", [publicKey: serviceConfig.sshPublicKey])
      def configFile = fs.resolveFile("${envPath}/${fileName}")
      // setup the file content and persist
      configFile.content.outputStream.withWriter { it.write(parsedFile) }
      configFile.createFile()
    }

    // place all of the regular files in the env directory
    def tfFiles = fs.resolveFile('res:terraform').children.findAll { it.file }
    tfFiles.each {
      def fileName = it.name.baseName
      def configFile = fs.resolveFile("${envPath}/${fileName}")
      // setup the file content and persist
      def content = it.content.inputStream.text
      configFile.content.outputStream.withWriter { it.write(content) }
      configFile.createFile()
    }
  }
}
