package io.cratekube.cloud.model.terraform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import groovy.transform.Canonical

@Canonical
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy)
class TerraformState {
  Integer version
  String terraformVersion
  Integer serial
  String lineage
  List<TerraformResource> resources
}
