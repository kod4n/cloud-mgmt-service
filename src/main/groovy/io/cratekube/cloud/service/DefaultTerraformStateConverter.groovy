package io.cratekube.cloud.service

import io.cratekube.cloud.api.TerraformStateConverter
import io.cratekube.cloud.model.ManagedResource
import io.cratekube.cloud.model.Status
import io.cratekube.cloud.model.terraform.TerraformResource

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require

/**
 * Default implementation for the {@link TerraformStateConverter} API.
 */
class DefaultTerraformStateConverter implements TerraformStateConverter {
  @Override
  List<ManagedResource> convert(List<TerraformResource> resources) {
    require resources, notNullValue()

    return resources.collectMany { resource ->
      def instances = resource.instances
      def type = resource.type
      def metadataProvider = AwsMetadataProviders.getMetadataProvider(type)
      def name = instances.size() == 1 ? resource.name : null
      instances?.collect { instance ->
        new ManagedResource(
          id: instance.attributes.id,
          name: name ?: instance.attributes.tags.Name,
          type: type,
          status: Status.APPLIED,
          metadata: metadataProvider.getMetadata(instance.attributes)
        )
      }
    }
  }
}
