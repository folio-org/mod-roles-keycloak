package org.folio.roles.integration.keyclock.client;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.TokenRequest;
import org.folio.roles.integration.keyclock.model.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A Keycloak feign client for operations with access token.
 */
@FeignClient(
  name = "keycloak-token-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class
)
public interface AccessTokenClient {

  /**
   * Retrieve access token in Keycloak.
   *
   * @param request {@link TokenRequest} object with parameters.
   * @return response {@link TokenResponse} with access token details.
   */
  @PostMapping(value = "/realms/master/protocol/openid-connect/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse retrieveAccessToken(@RequestBody TokenRequest request);
}
