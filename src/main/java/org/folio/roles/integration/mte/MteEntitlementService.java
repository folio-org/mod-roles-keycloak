package org.folio.roles.integration.mte;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@Service
@RequiredArgsConstructor
public class MteEntitlementService {

  private static final int PAGE_SIZE = 500;

  private final TenantEntitlementsClient client;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Returns the set of application IDs entitled for the current tenant.
   *
   * <p>Results are cached per tenant. Fetches up to {@value PAGE_SIZE} applications in a single request.
   * A WARN is logged if MTE reports more results than the page size, as only the first page is returned.
   *
   * @return set of versioned application IDs (e.g., {@code my-app-1.0.0})
   * @throws RuntimeException if the MTE client call fails
   */
  @Cacheable(cacheNames = "tenant-entitled-applications", key = "@folioExecutionContext.tenantId")
  public Set<String> getEntitledApplicationIdsForCurrentTenant() {
    var tenant = folioExecutionContext.getTenantId();
    var token = folioExecutionContext.getToken();
    var response = client.findEntitledApplicationsByTenantName(tenant, token, tenant, PAGE_SIZE, 0);
    if (response.getTotalRecords() != null && response.getTotalRecords() > PAGE_SIZE) {
      log.warn("MTE returned totalRecords={} for tenant '{}', but only {} were fetched; "
        + "entitled-only filtering may be incomplete", response.getTotalRecords(), tenant, PAGE_SIZE);
    }
    return toStream(response.getApplicationDescriptors())
      .map(MteApplicationDescriptor::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
