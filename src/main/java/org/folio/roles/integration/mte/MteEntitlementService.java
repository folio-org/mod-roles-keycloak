package org.folio.roles.integration.mte;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.folio.roles.integration.mte.model.MteApplicationDescriptor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for resolving the set of entitled application IDs for the current tenant.
 *
 * <p>Results are cached per tenant with a short TTL (configurable via
 * {@code TENANT_ENTITLED_APPLICATIONS_CACHE_TTL}). MTE client failures are propagated
 * — callers are responsible for fallback behavior.
 */
@Service
@RequiredArgsConstructor
public class MteEntitlementService {

  private final TenantEntitlementsClient client;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Returns the set of application IDs entitled for the current tenant.
   *
   * <p>Results are cached per tenant. Calls MTE with limit=500 and offset=0.
   *
   * @return set of versioned application IDs (e.g., {@code my-app-1.0.0})
   * @throws RuntimeException if the MTE client call fails
   */
  @Cacheable(cacheNames = "tenant-entitled-applications", key = "@folioExecutionContext.tenantId")
  public Set<String> getEntitledApplicationIdsForCurrentTenant() {
    var tenant = folioExecutionContext.getTenantId();
    var token = folioExecutionContext.getToken();
    var response = client.findEntitledApplicationsByTenantName(tenant, token, tenant, 500, 0);
    return toStream(response.getApplicationDescriptors())
      .map(MteApplicationDescriptor::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
