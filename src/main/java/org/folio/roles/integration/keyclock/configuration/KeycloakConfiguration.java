package org.folio.roles.integration.keyclock.configuration;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import lombok.RequiredArgsConstructor;
import org.folio.roles.integration.keyclock.RealmConfigurationProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KeycloakConfiguration {

  private final KeycloakConfigurationProperties configuration;
  private final RealmConfigurationProvider realmConfigurationProvider;

  @Bean
  public Keycloak keycloakAdminClient() {
    var realmProvider = realmConfigurationProvider.getRealmConfiguration();
    return KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(configuration.getBaseUrl())
      .clientId(realmProvider.getClientId())
      .username("")
      .password("")
      .clientSecret(stripToNull(realmProvider.getClientSecret()))
      .grantType(configuration.getGrantType())
      .build();
  }
}
