package org.folio.roles.integration.keyclock;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.client.ClientManagerClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Keycloak client service for operation with Keycloak clients.
 */
@Log4j2
@Service
@AllArgsConstructor
public class KeycloakClientService {

  private final ClientManagerClient client;
  private final Keycloak keycloak;
  private final KeycloakConfigurationProperties properties;
  private final FolioExecutionContext context;
  private final KeycloakAccessTokenService tokenService;

  /**
   * This method finds the client ID and caches it using the specified cache and key generator.
   * The method is annotated with {@link Cacheable} which indicates that the method's result will be cached.
   *
   * @return the client ID that was found.
   */
  @Cacheable(cacheNames = "keycloak-client-id", key = "@folioExecutionContext.tenantId")
  public String findAndCacheLoginClientUuid() {
    var clientName = context.getTenantId() + properties.getLogin().getClientNameSuffix();
    var response = client.findClientsByClientId(
      tokenService.getToken(), context.getTenantId(), clientName);

    if (isNotEmpty(response) && response.size() > INTEGER_ONE) {
      throw new IllegalStateException("More than 1 clients were found for name: " + clientName);
    }

    var keycloakClient = response.stream().findFirst().orElseThrow(
      () -> new NotFoundException("Failed to find client ID by clientId"));
    log.debug("Client ID found: ID = {}, name = {}, tenantId = {}", keycloakClient.getClientId(), clientName,
      context.getTenantId());
    return keycloakClient.getId();
  }

  public ClientRepresentation getLoginClient() {
    var tenantId = context.getTenantId();
    var realmResource = keycloak.realm(tenantId);

    var loginClientId = tenantId + properties.getLogin().getClientNameSuffix();
    var clientsResource = realmResource.clients();
    var loginClients = clientsResource.findByClientId(loginClientId);

    return loginClients.stream()
      .filter(cl -> cl.getClientId().equals(loginClientId))
      .findFirst()
      .orElseThrow(() -> new EntityNotFoundException(String.format("Client '%s' not found.", loginClientId)));
  }
}
