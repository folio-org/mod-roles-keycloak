package org.folio.roles.integration.kafka.filter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

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

  public Set<String> getEnabledTenants() throws TenantsNotEnabledException {
    var result = tenantEntitlementClient.lookupTenantsByModuleId(moduleId);
    if (isEmpty(result)) {
      throw TenantsNotEnabledException.forModule(moduleId);
    }

    log.debug("Tenants entitled for module: {}", result);
    return result;
  }
}
