package org.folio.roles.integration.keyclock.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * Keycloak access token details.
 */
@Data
@JsonNaming(SnakeCaseStrategy.class)
public class TokenResponse {

  private String accessToken;
  private Long expiresIn;
  private Long refreshExpiresIn;
  private String refreshToken;
  private String tokenType;
}
