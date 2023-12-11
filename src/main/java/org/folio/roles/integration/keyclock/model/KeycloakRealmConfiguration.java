package org.folio.roles.integration.keyclock.model;

import lombok.Data;

@Data
public class KeycloakRealmConfiguration {

  /**
   * Keycloak realm client id.
   */
  private String clientId;

  /**
   * Keycloak realm client secret.
   */
  private String clientSecret;

  /**
   * Sets clientId and returns {@link org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration} object.
   *
   * @param clientId - keycloak client id.
   * @return {@link org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration} object
   */
  public KeycloakRealmConfiguration clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets clientSecret and returns {@link org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration} object.
   *
   * @param clientSecret - keycloak client secret.
   * @return {@link org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration} object
   */
  public KeycloakRealmConfiguration clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }
}
