package io.cratekube.auth

import io.dropwizard.auth.Auth
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit5.ResourceExtension
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature
import spock.lang.Specification

import javax.annotation.security.RolesAllowed
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

import static org.hamcrest.Matchers.equalTo
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class ApiKeyAuthBundleSpec extends Specification {
  static final OAuthCredentialAuthFilter<User> authFilter = new OAuthCredentialAuthFilter.Builder<User>().with {
    prefix = 'Bearer'
    authenticator = new ApiKeyAuthenticator<User>(
      (key) -> new ApiKey(name: 'keyname', key: 'keyvalue', roles: ['admin']),
      (key) -> new User(name: 'keyname')
    )
    authorizer = new ApiKeyAuthorizer<User>(
      (key) -> new ApiKey(name: 'keyname', key: 'keyvalue', roles: ['admin'])
    )
    buildAuthFilter()
  }

  public static final ResourceExtension resource = ResourceExtension.builder()
    .addProvider(new AuthDynamicFeature(authFilter))
    .addProvider(RolesAllowedDynamicFeature)
    .addResource(new ApiKeyBundleTestResource())
    .build()

  def setupSpec() {
    resource.before()
  }

  def cleanupSpec() {
    resource.after()
  }

  def 'should access insecure path without authorization header'() {
    when:
    def result = resource.target('/test/insecure').request().get()

    then:
    expect result.status, equalTo(200)
    verifyAll(result.readEntity(String)) {
      expect it, notEmptyString()
      expect it, equalTo('insecure')
    }
  }

  def 'should fail to access secure path without authorization header'() {
    when:
    def result = resource.target('/test/secure').request().get()

    then:
    expect result.status, equalTo(401)
  }

  def 'should access secure path when using authorization header'() {
    when:
    def result = resource.target('/test/secure')
      .request()
      .header(HttpHeaders.AUTHORIZATION, 'Bearer keyvalue')
      .get()

    then:
    expect result.status, equalTo(200)
    verifyAll(result.readEntity(String)) {
      expect it, notEmptyString()
      expect it, equalTo('secure')
    }
  }

  def 'should access secure path with roles when using authorization header'() {
    when:
    def result = resource.target('/test/secure/role')
      .request()
      .header(HttpHeaders.AUTHORIZATION, 'Bearer keyvalue')
      .get()

    then:
    expect result.status, equalTo(200)
    verifyAll(result.readEntity(String)) {
      expect it, notEmptyString()
      expect it, equalTo('secure')
    }
  }

  def 'should fail to access secure path with unknown role when using authorization header'() {
    when:
    def result = resource.target('/test/secure/invalidrole')
      .request()
      .header(HttpHeaders.AUTHORIZATION, 'Bearer keyvalue')
      .get()

    then:
    expect result.status, equalTo(403)
  }

  @Path('test')
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  static final class ApiKeyBundleTestResource {
    @GET
    @Path('insecure')
    String insecure() {
      return 'insecure'
    }

    @GET
    @Path('secure')
    String secure(@Auth User user) {
      return 'secure'
    }

    @GET
    @Path('secure/role')
    @RolesAllowed('admin')
    String secureWithRole(@Auth User user) {
      return 'secure'
    }

    @GET
    @Path('secure/invalidrole')
    @RolesAllowed('other-admin')
    String secureWithInvalidRole(@Auth User user) {
      return 'secure'
    }
  }
}
