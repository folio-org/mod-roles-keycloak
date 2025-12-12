package org.folio.roles.integration.keyclock;

import lombok.RequiredArgsConstructor;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration;
import org.folio.spring.FolioExecutionContext;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealmConfigurationProvider {

  private static final String REALM = "master";
  private final SecureStore secureStore;
  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final SecureStoreProperties secureStoreProperties;

  /**
   * Provides realm configuration using {@link FolioExecutionContext} object.
   *
   * @return {@link KeycloakRealmConfiguration} object for user authentication
   */
  @Cacheable(cacheNames = "keycloak-configuration", key = "'keycloak-config'")
  public KeycloakRealmConfiguration getRealmConfiguration() {
    var clientId = keycloakConfigurationProperties.getClientId();
    return new KeycloakRealmConfiguration()
      .clientId(clientId)
      .clientSecret(retrieveKcClientSecret(clientId));
  }

  private String retrieveKcClientSecret(String clientId) {
    try {
      return secureStore.get(buildKey(secureStoreProperties.getEnvironment(), clientId));
    } catch (SecretNotFoundException e) {
      throw new IllegalStateException(String.format(
        "Failed to get value from secure store [clientId: %s]", clientId), e);
    }
  }

  private String buildKey(String env, String clientId) {
    return String.format("%s_%s_%s", env, REALM, clientId);
  }
}
