package org.folio.roles.integration.keyclock;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.stereotype.Service;

/**
 * Keycloak client service for operation with Keycloak clients.
 */
@Log4j2
@Service
@AllArgsConstructor
public class KeycloakClientService {

  private final Keycloak keycloak;
  private final FolioExecutionContext context;
  private final KeycloakConfigurationProperties properties;

  /**
   * Returns login client representation for tenant.
   *
   * @return login client as {@link ClientRepresentation} object
   */
  public ClientRepresentation getLoginClient() {
    var tenantId = context.getTenantId();
    var realmResource = keycloak.realm(tenantId);

    var loginClientId = tenantId + properties.getLogin().getClientNameSuffix();
    var clientsResource = realmResource.clients();
    var loginClients = clientsResource.findByClientId(loginClientId);

    return loginClients.stream()
      .filter(loginClient -> loginClient.getClientId().equals(loginClientId))
      .findFirst()
      .orElseThrow(() -> new EntityNotFoundException(String.format("Client '%s' not found.", loginClientId)));
  }
}
