package org.folio.roles.integration.keyclock.model;

import feign.form.FormProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Keycloak access token request parameters.
 */
@Data
@Builder
public class TokenRequest {

  @FormProperty("client_id")
  private String clientId;
  @FormProperty("client_secret")
  private String clientSecret;
  @FormProperty("grant_type")
  private String grantType;
}
