package org.folio.roles.integration.keyclock.client;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.KeycloakClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * A Keycloak feign client for operations with policies.
 */
@FeignClient(name = "keycloak-client-manager-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class)
public interface ClientManagerClient {

  @GetMapping(value = "/admin/realms/{realm}/clients?clientId={clientId}")
  List<KeycloakClient> findClientsByClientId(@RequestHeader(AUTHORIZATION) String token, @PathVariable String realm,
                                               @PathVariable String clientId);
}
