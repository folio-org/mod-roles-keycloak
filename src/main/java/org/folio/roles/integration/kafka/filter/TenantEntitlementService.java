package org.folio.roles.integration.kafka.filter;

import java.util.Objects;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TenantEntitlementService {

  private final String moduleId;
  private final TenantEntitlementClient tenantEntitlementClient;

  public TenantEntitlementService(String moduleId, TenantEntitlementClient tenantEntitlementClient) {
    Objects.requireNonNull(moduleId, "Module ID must not be null");
    this.moduleId = moduleId;
    this.tenantEntitlementClient = tenantEntitlementClient;
  }

  public Set<String> getEnabledTenants() throws TenantsAreDisabledException {
    var result = tenantEntitlementClient.lookupTenantsByModuleId(moduleId);
    log.debug("Tenants entitled for module: {}", result);
    return result;
  }
}
