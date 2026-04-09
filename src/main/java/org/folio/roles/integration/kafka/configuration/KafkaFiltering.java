package org.folio.roles.integration.kafka.configuration;

import static org.folio.roles.integration.kafka.filter.DisabledTenantStrategy.FAIL;
import static org.folio.roles.integration.kafka.filter.DisabledTenantStrategy.SKIP;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.folio.roles.integration.kafka.filter.DisabledTenantStrategy;

@Data
public class KafkaFiltering {

  private TenantFilter tenantFilter = new TenantFilter();

  @Data
  public static class TenantFilter {

    private boolean enabled = false; // should be explicitly enabled
    private boolean ignoreEmptyBatch = true;
    private @NotNull DisabledTenantStrategy tenantDisabledStrategy = SKIP;
    private @NotNull DisabledTenantStrategy allTenantsDisabledStrategy = FAIL;
  }
}
