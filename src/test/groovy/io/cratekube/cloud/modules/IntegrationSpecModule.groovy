package io.cratekube.cloud.modules

import com.google.inject.Provides
import io.cratekube.cloud.AppConfig
import io.cratekube.cloud.api.EnvironmentManager
import io.dropwizard.client.JerseyClientBuilder
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule
import spock.mock.DetachedMockFactory
import spock.mock.MockFactory

import javax.inject.Singleton
import javax.ws.rs.client.Client

/**
 * Guice module used for integration specs.
 */
class IntegrationSpecModule extends DropwizardAwareModule<AppConfig> {
  MockFactory factory = new DetachedMockFactory()

  @Override
  protected void configure() {
    bind EnvironmentManager toInstance factory.Mock(EnvironmentManager)
  }

  @Provides
  @Singleton
  Client clientProvider() {
    return new JerseyClientBuilder(environment()).using(configuration().jerseyClient).build('external-client')
  }
}
