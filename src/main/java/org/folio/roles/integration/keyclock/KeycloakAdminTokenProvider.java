package org.folio.roles.integration.keyclock;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Instant;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.client.KeycloakTokenClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Acquires and caches the Keycloak admin access token used to authenticate {@code KeycloakAdminClient} calls.
 *
 * <p>Replaces the token lifecycle that {@code keycloak-admin-client} handled internally: it requests a token
 * from the {@code master} realm token endpoint with the module's configured client grant and refreshes it
 * shortly before expiry. The client secret is resolved lazily (from the secure store, via a supplier) so no
 * Keycloak call is made at application start-up.</p>
 */
@Log4j2
public class KeycloakAdminTokenProvider {

  private static final long EXPIRY_SKEW_SECONDS = 30L;

  private final KeycloakTokenClient tokenClient;
  private final KeycloakConfigurationProperties properties;
  private final Supplier<String> clientSecretSupplier;

  private String accessToken;
  private Instant expiresAt = Instant.MIN;

  public KeycloakAdminTokenProvider(KeycloakTokenClient tokenClient, KeycloakConfigurationProperties properties,
    Supplier<String> clientSecretSupplier) {
    this.tokenClient = tokenClient;
    this.properties = properties;
    this.clientSecretSupplier = clientSecretSupplier;
  }

  /**
   * Returns a valid admin access token, fetching a new one if none is cached or the cached one has expired.
   *
   * @return a bearer access token for Keycloak admin requests
   */
  public synchronized String getAccessToken() {
    if (accessToken == null || Instant.now().isAfter(expiresAt)) {
      refresh();
    }
    return accessToken;
  }

  /**
   * Forces acquisition of a fresh admin access token, discarding any cached value.
   *
   * @return the newly obtained bearer access token
   */
  public synchronized String refreshToken() {
    refresh();
    return accessToken;
  }

  private void refresh() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", properties.getGrantType());
    form.add("client_id", properties.getClientId());
    var secret = clientSecretSupplier.get();
    if (isNotBlank(secret)) {
      form.add("client_secret", secret);
    }

    log.debug("Obtaining Keycloak admin token: clientId={}, grantType={}",
      properties.getClientId(), properties.getGrantType());
    var response = tokenClient.obtainToken(form);
    this.accessToken = response.getAccessToken();
    var ttl = response.getExpiresIn() != null ? response.getExpiresIn() : 0L;
    this.expiresAt = Instant.now().plusSeconds(Math.max(0L, ttl - EXPIRY_SKEW_SECONDS));
  }
}
