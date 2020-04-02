package io.cratekube.cloud.modules

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import com.google.inject.Provides
import io.cratekube.cloud.AppConfig
import io.cratekube.cloud.api.EnvironmentManager
import io.cratekube.cloud.api.ProcessExecutor
import io.cratekube.cloud.api.TemplateProcessor
import io.cratekube.cloud.api.TerraformApi
import io.cratekube.cloud.api.TerraformStateConverter
import io.cratekube.cloud.modules.annotation.TerraformCmd
import io.cratekube.cloud.service.DefaultTerraformStateConverter
import io.cratekube.cloud.service.HandlebarsTemplateProcessor
import io.cratekube.cloud.service.TerraformCommand
import io.cratekube.cloud.service.TerraformEnvironmentManager
import io.cratekube.cloud.service.TerraformService
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.VFS
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Default module to be used when running this application.
 */
class ProductionModule extends DropwizardAwareModule<AppConfig> {
  @Override
  protected void configure() {
    bind TerraformApi to TerraformService
    bind TerraformStateConverter to DefaultTerraformStateConverter
    bind TemplateProcessor to HandlebarsTemplateProcessor
    bind ProcessExecutor annotatedWith TerraformCmd to TerraformCommand
    bind EnvironmentManager to TerraformEnvironmentManager
    bind FileSystemManager toInstance VFS.manager
    bind Executor toInstance Executors.newCachedThreadPool()
  }

  @Provides
  static Handlebars handlebarsProvider() {
    return new Handlebars().with { registerHelpers StringHelpers }
  }
}
