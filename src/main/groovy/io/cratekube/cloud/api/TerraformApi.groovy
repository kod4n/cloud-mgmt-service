package io.cratekube.cloud.api

import io.cratekube.cloud.model.terraform.TerraformResource
import org.apache.commons.vfs2.FileObject

/**
 * Base interface for terraform operations.
 */
interface TerraformApi {
  /**
   * Initializes a directory with any configured terraform plugins.
   *
   * @param directory {@code non-null} file object directory
   */
  void init(FileObject directory)

  /**
   * Creates a plan and applies the plan for a given directory.
   *
   * @param directory {@code non-null} file object directory
   */
  void apply(FileObject directory)

  /**
   * Destroys all created resources within a directory.
   *
   * @param directory {@code non-null} file object directory
   */
  void destroy(FileObject directory)

  /**
   * Pulls the current terraform state and returns any resources that have been
   * provisioned.
   *
   * @param directory {@code non-null} file object directory
   * @return list of terraform resources found in state
   */
  List<TerraformResource> state(FileObject directory)
}
