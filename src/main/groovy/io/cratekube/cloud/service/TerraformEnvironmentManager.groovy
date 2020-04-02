package io.cratekube.cloud.service

import groovy.util.logging.Slf4j
import io.cratekube.cloud.ServiceConfig
import io.cratekube.cloud.api.EnvironmentAlreadyExistsException
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.api.EnvironmentNotFoundException
import io.cratekube.cloud.api.TerraformApi
import io.cratekube.cloud.api.TerraformStateConverter
import io.cratekube.cloud.model.Constants
import io.cratekube.cloud.model.Environment
import io.cratekube.cloud.model.Status
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
  Executor executor
  TerraformApi terraform
  TerraformStateConverter terraformStateConverter
  FileSystemManager fs
  ServiceConfig serviceConfig

  @Inject
  TerraformEnvironmentManager(Executor executor, TerraformApi terraform, TerraformStateConverter terraformStateConverter,
                              FileSystemManager fs, @Config ServiceConfig serviceConfig) {
    this.executor = require executor, notNullValue()
    this.terraform = require terraform, notNullValue()
    this.terraformStateConverter = require terraformStateConverter, notNullValue()
    this.fs = require fs, notNullValue()
    this.serviceConfig = require serviceConfig, notNullValue()
  }

  @Override
  Environment create(EnvironmentRequest environmentRequest) throws EnvironmentAlreadyExistsException {
    require environmentRequest, notNullValue()

    def environmentName = environmentRequest.name

    // check to see if an environment directory already exists for this env
    // throw exception if the environments dir is found
    def envDir = resolveEnvDirectory(environmentName)
    if (envDir.exists()) {
      def exception = new EnvironmentAlreadyExistsException("environment ${environmentName} already exists")
      log.debug 'environment [{}] already exists, cannot create', environmentName, exception
      throw exception
    }

    // create the environment directory
    envDir.createFolder()

    // start the environment creation in the executor service
    executor.execute {
      terraform.with {
        // init environment and apply terraform files
        init envDir
        apply envDir
      }
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
    def tfResources = terraform.state(envDir)

    return new Environment(
      name: envName,
      provider: serviceConfig.provider,
      status: Status.APPLIED,
      resources: terraformStateConverter.convert(tfResources)
    )
  }

  @Override
  void deleteByName(String environmentName) throws EnvironmentNotFoundException {
    require environmentName, notEmptyString()

    // find all environments provisioned via terraform state
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
      terraform.destroy(envDir)
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
}
