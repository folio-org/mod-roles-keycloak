package org.folio.roles.integration.keyclock.client;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring HTTP-interface client for the Keycloak {@code master} realm token endpoint.
 *
 * <p>Acquires the admin access token used to authenticate {@link KeycloakAdminClient} requests, replacing the
 * token acquisition that {@code keycloak-admin-client}'s {@code TokenManager} performed internally.</p>
 */
@HttpExchange
public interface KeycloakTokenClient {

  /**
   * Obtains an access token from the {@code master} realm token endpoint using the supplied OAuth2 form grant.
   *
   * @param formData - OAuth2 form parameters (grant_type, client_id, client_secret, ...)
   * @return the token response containing the access token and its lifetime
   */
  @PostExchange(value = "/realms/master/protocol/openid-connect/token", contentType = APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse obtainToken(@RequestBody MultiValueMap<String, String> formData);
}
