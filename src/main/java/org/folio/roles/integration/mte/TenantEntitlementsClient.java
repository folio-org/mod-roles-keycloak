package org.folio.roles.integration.mte;

import org.folio.roles.integration.mte.model.MteApplicationDescriptors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HTTP client for the mgr-tenant-entitlements service.
 */
@HttpExchange(url = "entitlements")
public interface TenantEntitlementsClient {

  /**
   * Returns the list of application descriptors entitled for the given tenant.
   *
   * @param tenantName - Okapi tenant name
   * @param token      - Okapi authentication token (x-okapi-token header)
   * @param tenant     - Okapi tenant header (x-okapi-tenant)
   * @param limit      - maximum number of records to return
   * @param offset     - skip this many records
   * @return wrapper containing application descriptors
   */
  @GetExchange("/{tenantName}/applications")
  MteApplicationDescriptors findEntitledApplicationsByTenantName(
    @PathVariable("tenantName") String tenantName,
    @RequestHeader("x-okapi-token") String token,
    @RequestHeader("x-okapi-tenant") String tenant,
    @RequestParam("limit") int limit,
    @RequestParam("offset") int offset);
}
