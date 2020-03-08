package io.cratekube.cloud.service

import io.cratekube.cloud.api.DefaultProcessExecutor

/**
 * Command implementation for the terraform binary.
 */
class TerraformCommand extends DefaultProcessExecutor {
  String executablePath = '/bin/terraform'
}
