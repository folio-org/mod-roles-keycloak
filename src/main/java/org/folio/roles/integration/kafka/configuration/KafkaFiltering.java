package org.folio.roles.integration.kafka.configuration;

import static org.folio.roles.integration.kafka.filter.TenantsNotEnabledStrategy.FAIL;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.folio.roles.integration.kafka.filter.TenantsNotEnabledStrategy;

@Data
public class KafkaFiltering {

  private TenantFilter tenantFilter = new TenantFilter();

  @Data
  public static class TenantFilter {

    private boolean enabled = false; // should be explicitly enabled
    private boolean ignoreEmptyBatch = true;
    private @NotNull TenantsNotEnabledStrategy tenantsNotEnabledStrategy = FAIL;
  }
}
