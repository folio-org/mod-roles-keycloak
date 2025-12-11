package org.folio.roles.integration.keyclock.configuration;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.folio.common.utils.tls.FeignClientTlsUtils.buildSslContext;
import static org.folio.common.utils.tls.Utils.IS_HOSTNAME_VERIFICATION_DISABLED;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.roles.integration.keyclock.RealmConfigurationProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class KeycloakConfiguration {

  private static final DefaultHostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();

  private final KeycloakConfigurationProperties configuration;
  private final RealmConfigurationProvider realmConfigurationProvider;

  @Bean
  public Keycloak keycloakAdminClient() {
    var realmConfiguration = realmConfigurationProvider.getRealmConfiguration();
    return buildKeycloakAdminClient(realmConfiguration.getClientSecret(), configuration);
  }

  private static Keycloak buildKeycloakAdminClient(String clientSecret, KeycloakConfigurationProperties properties) {
    var builder = KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(properties.getBaseUrl())
      .clientId(properties.getClientId())
      .clientSecret(stripToNull(clientSecret))
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
