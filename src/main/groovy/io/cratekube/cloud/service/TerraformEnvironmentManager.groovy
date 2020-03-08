package io.cratekube.cloud.service

import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.model.Environment
import io.cratekube.cloud.modules.annotation.TerraformCmd
import io.cratekube.cloud.resources.EnvironmentResource.EnvironmentRequest
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config

import javax.inject.Inject
import java.util.concurrent.ExecutorService

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation for the {@link EnvironmentManager}.
 */
class TerraformEnvironmentManager implements EnvironmentManager {
  ProcessExecutor terraform
  ExecutorService executorService
  String cloudProvider

  @Inject
  TerraformEnvironmentManager(@TerraformCmd ProcessExecutor terraform, ExecutorService executorService,
                              @Config('provider') String cloudProvider) {
    this.terraform = require terraform, notNullValue()
    this.executorService = require executorService, notNullValue()
    this.cloudProvider = require cloudProvider, notEmptyString()
  }

  @Override
  Environment create(EnvironmentRequest environmentRequest) {
    require environmentRequest, notNullValue()
    // build out the resources that will be provisioned from the terraform templates
    // create the environment object based on items to be provisioned
    // kick off creation of items via executorService
    // return environment reference
    return null
  }

  @Override
  List<Environment> getAll() {
    // find all environments provisioned via terraform state
    return []
  }

  @Override
  Optional<Environment> findByName(String environmentName) {
    require environmentName, notEmptyString()
    return Optional.empty()
  }

  @Override
  void deleteByName(String environmentName) {
    require environmentName, notEmptyString()
  }
}
