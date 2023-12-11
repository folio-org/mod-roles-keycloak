package org.folio.roles.integration.keyclock;

import static org.apache.commons.lang3.StringUtils.SPACE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.client.AccessTokenClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.model.TokenRequest;
import org.folio.roles.integration.keyclock.model.TokenResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Access token service for operations with Keycloak access token.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakAccessTokenService {

  private final AccessTokenClient client;
  private final KeycloakConfigurationProperties properties;
  private final RealmConfigurationProvider realmConfigurationProvider;

  /**
   * Provides access token for communication with Keycloak API.
   */
  @Cacheable(cacheNames = "keycloak-access-token", key = "'client-token'")
  public String getToken() {
    var token = retrieve();
    return token.getTokenType() + SPACE + token.getAccessToken();
  }

  private TokenResponse retrieve() {
    var realmConfig = realmConfigurationProvider.getRealmConfiguration();
    var request = buildTokenRequest(realmConfig.getClientId(), realmConfig.getClientSecret());
    var token = client.retrieveAccessToken(request);
    log.info("Keycloak access token has been retrieved: clientId = {}", realmConfig.getClientId());
    return token;
  }

  private TokenRequest buildTokenRequest(String clientId, String clientSecret) {
    return TokenRequest.builder().clientId(clientId).clientSecret(clientSecret)
      .grantType(properties.getGrantType()).build();
  }
}
