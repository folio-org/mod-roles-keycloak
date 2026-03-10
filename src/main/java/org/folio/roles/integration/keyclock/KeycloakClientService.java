package org.folio.roles.integration.keyclock;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
   * <p>The result is cached per tenant to avoid redundant Keycloak API calls,
   * especially during parallel permission processing where this method would
   * otherwise be invoked once per endpoint.</p>
   *
   * @return login client as {@link ClientRepresentation} object
   */
  @Cacheable(cacheNames = "keycloak-login-client", key = "@folioExecutionContext.tenantId")
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

  /**
   * Evicts the cached login client for a given tenant.
   *
   * <p>Should be called on tenant deletion to prevent stale client data
   * from remaining in the cache after the realm is removed from Keycloak.</p>
   *
   * @param tenantId - the tenant identifier whose cached login client should be evicted
   */
  @CacheEvict(cacheNames = "keycloak-login-client", key = "#tenantId")
  public void evictLoginClient(String tenantId) {
    log.info("Evicted login client cache for tenant: {}", tenantId);
  }
}
