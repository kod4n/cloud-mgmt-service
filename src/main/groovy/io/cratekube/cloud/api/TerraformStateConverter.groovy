package io.cratekube.cloud.api

import io.cratekube.cloud.model.ManagedResource
import io.cratekube.cloud.model.terraform.TerraformResource

/**
 * Converter for terraform state data.
 * {@link TerraformResource} objects represent data found from the terraform state, {@link ManagedResource} objects
 * are the data structure returned by this services API when viewing environment cloud resources.
 */
interface TerraformStateConverter {
  /**
   * Converts {@link TerraformResource} collections to {@link ManagedResource} collections.  Requires a non-null
   * collection of terraform resources.
   *
   * @param resources {@code non-null} collection of terraform resources
   * @return {@code non-null} list of converted managed resources
   */
  List<ManagedResource> convert(List<TerraformResource> resources)
}
