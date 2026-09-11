package org.folio.roles.integration.keyclock.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OAuth2 token response returned by the Keycloak {@code master} realm token endpoint.
 *
 * <p>Replaces the token payload that {@code keycloak-admin-client} deserialized internally; used by
 * {@link org.folio.roles.integration.keyclock.KeycloakAdminTokenProvider} to obtain and cache the admin
 * access token for {@link KeycloakAdminClient} calls.</p>
 */
@Data
public class KeycloakTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("token_type")
  private String tokenType;
}
