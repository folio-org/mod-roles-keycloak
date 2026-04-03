package org.folio.roles.integration.kafka.filter;

import static org.folio.roles.integration.kafka.filter.EnabledTenantMessageFilter.NoEnabledTenantsStrategy.FAIL;

import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Log4j2
public class EnabledTenantMessageFilter<K, V extends ResourceEvent> implements RecordFilterStrategy<K, V> {

  private final TenantEntitlementService tenantEntitlementService;
  private final boolean ignoreEmptyBatch;
  private final NoEnabledTenantsStrategy noEnabledTenantsStrategy;

  public EnabledTenantMessageFilter(TenantEntitlementService tenantEntitlementService, boolean ignoreEmptyBatch) {
    this(tenantEntitlementService, ignoreEmptyBatch, FAIL);
  }

  public EnabledTenantMessageFilter(TenantEntitlementService tenantEntitlementService, boolean ignoreEmptyBatch,
    NoEnabledTenantsStrategy noEnabledTenantsStrategy) {
    Objects.requireNonNull(noEnabledTenantsStrategy, "NoEnabledTenantsStrategy must not be null");
    this.tenantEntitlementService = tenantEntitlementService;
    this.ignoreEmptyBatch = ignoreEmptyBatch;
    this.noEnabledTenantsStrategy = noEnabledTenantsStrategy;
  }

  @Override
  public boolean filter(ConsumerRecord<K, V> consumerRecord) {
    var key = consumerRecord.key();
    var value = consumerRecord.value();
    var tenant = value.getTenant();

    log.debug("Filtering message for tenant: messageKey = {}, tenant = {}", key, tenant);

    var result = false;
    try {
      var enabledTenants = tenantEntitlementService.getEnabledTenants();
      result = !enabledTenants.contains(tenant);
    } catch (NoTenantsEnabledException e) {
      log.warn("No tenants are enabled for the module. Applying no enabled tenants strategy: {}",
        noEnabledTenantsStrategy);

      result = switch (noEnabledTenantsStrategy) {
        case ACCEPT -> false;
        case FILTER_OUT -> true;
        case FAIL -> throw e;
      };
    }

    log.debug("Message for tenant is {}: messageKey = {}, tenant = {}",
      result ? "filtered out" : "accepted", key, tenant);

    return result;
  }

  @Override
  public boolean ignoreEmptyBatch() {
    return ignoreEmptyBatch;
  }

  public enum NoEnabledTenantsStrategy {
    FAIL, ACCEPT, FILTER_OUT
  }
}
