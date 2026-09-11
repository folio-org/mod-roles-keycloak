package org.folio.roles.base;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.folio.common.utils.tls.Utils.IS_HOSTNAME_VERIFICATION_DISABLED;
import static org.folio.common.utils.tls.Utils.buildSslContext;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.roles.integration.keyclock.RealmConfigurationProvider;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only Keycloak admin client. Production and native code talk to Keycloak through the module's Spring
 * HTTP-interface client ({@code integration.keyclock.client.KeycloakAdminClient}); the RESTEasy
 * {@code keycloak-admin-client} is retained purely as an integration-test driver — the {@code it/*} suites and
 * {@link org.folio.roles.KeycloakTestClient} exercise a real Keycloak through this bean. It reproduces the
 * former production {@code Keycloak} bean so those tests keep working unchanged.
 */
@TestConfiguration
public class KeycloakAdminClientTestConfiguration {

  private static final DefaultHostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();

  @Bean
  public Keycloak keycloakAdminTestClient(KeycloakConfigurationProperties properties,
    RealmConfigurationProvider realmConfigurationProvider) {
    var realmConfiguration = realmConfigurationProvider.getRealmConfiguration();
    var builder = KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(properties.getBaseUrl())
      .clientId(properties.getClientId())
      .clientSecret(stripToNull(realmConfiguration.getClientSecret()))
      .grantType(properties.getGrantType());

    if (properties.getTls() != null && properties.getTls().isEnabled()) {
      builder.resteasyClient(buildResteasyClient(properties.getTls()));
    }
    return builder.build();
  }

  private static ResteasyClient buildResteasyClient(TlsProperties properties) {
    return (ResteasyClient) newBuilder()
      .sslContext(buildSslContext(properties))
      .hostnameVerifier(IS_HOSTNAME_VERIFICATION_DISABLED ? NoopHostnameVerifier.INSTANCE : DEFAULT_HOSTNAME_VERIFIER)
      .register(JacksonProvider.class)
      .build();
  }
}
