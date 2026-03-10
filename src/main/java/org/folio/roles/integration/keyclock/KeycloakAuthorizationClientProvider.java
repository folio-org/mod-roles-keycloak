package org.folio.roles.integration.keyclock;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakAuthorizationClientProvider {

  private final Keycloak keycloak;
  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final FolioExecutionContext context;
  private final KeycloakClientService keycloakClientService;

  @Cacheable(value = "authorization-client-cache", key = "@folioExecutionContext.tenantId")
  public AuthorizationResource getAuthorizationClient() {
    return createAuthorizationClient();
  }

  /**
   * Creates a new (non-cached) {@link AuthorizationResource} client.
   *
   * <p>Use this for concurrent/parallel operations where sharing a single cached client instance
   * across threads is unsafe. Each caller receives an independent proxy instance.</p>
   */
  public AuthorizationResource createAuthorizationClient() {
    var tenantId = context.getTenantId();
    var client = keycloakClientService.getLoginClient();
    return keycloak.proxy(AuthorizationResource.class, authorizationResourceUri(client, tenantId));
  }

  /**
   * Evicts the cached {@link AuthorizationResource} for a given tenant.
   *
   * <p>Should be called on tenant deletion to prevent stale proxy instances
   * from remaining in the cache after the realm is removed from Keycloak.</p>
   *
   * @param tenantId - the tenant identifier whose cached client should be evicted
   */
  @CacheEvict(value = "authorization-client-cache", key = "#tenantId")
  public void evictAuthorizationClient(String tenantId) {
    log.info("Evicted authorization client cache for tenant: {}", tenantId);
  }

  private URI authorizationResourceUri(ClientRepresentation client, String realmName) {
    // Strip trailing slash to prevent double-slash in URI if baseUrl is misconfigured
    var baseUrl = StringUtils.stripEnd(keycloakConfigurationProperties.getBaseUrl(), "/");
    return URI.create(String.format("%s/admin/realms/%s/clients/%s/authz/resource-server",
      baseUrl, realmName, client.getId()));
  }
}
