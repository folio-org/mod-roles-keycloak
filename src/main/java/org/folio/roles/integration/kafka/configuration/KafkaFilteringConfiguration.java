package org.folio.roles.integration.kafka.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.configuration.KafkaFiltering.TenantFilter;
import org.folio.roles.integration.kafka.filter.EnabledTenantMessageFilter;
import org.folio.roles.integration.kafka.filter.TenantEntitlementClient;
import org.folio.roles.integration.kafka.filter.TenantEntitlementService;
import org.folio.roles.integration.kafka.filter.mmd.ModuleMetadata;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Log4j2
@Configuration
public class KafkaFilteringConfiguration {

  @ConditionalOnProperty(value = "application.kafka.filtering.tenant-filter.enabled")
  @RequiredArgsConstructor
  public static class TenantFilterConfiguration {

    private final TenantFilter tenantFilter;

    @Bean
    public TenantEntitlementClient tenantEntitlementClient(HttpServiceProxyFactory factory) {
      return factory.createClient(TenantEntitlementClient.class);
    }

    @Bean
    public TenantEntitlementService tenantEntitlementService(TenantEntitlementClient tenantEntitlementClient,
      ModuleMetadata moduleMetadata) {
      var moduleId = moduleMetadata.getModuleId();
      return new TenantEntitlementService(moduleId, tenantEntitlementClient);
    }

    @Bean("tenantAwareMessageFilter")
    public <K, V extends ResourceEvent> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
      TenantEntitlementService tenantEntitlementService) {
      return new EnabledTenantMessageFilter<>(
        tenantEntitlementService,
        tenantFilter.isIgnoreEmptyBatch(),
        tenantFilter.getTenantsNotEnabledStrategy()
      );
    }
  }

  @ConditionalOnProperty(value = "application.kafka.filtering.tenant-filter.enabled",
    havingValue = "false",
    matchIfMissing = true)
  @RequiredArgsConstructor
  public static class DisabledTenantFilterConfiguration {

    @Bean("tenantAwareMessageFilter")
    public <K, V extends ResourceEvent> RecordFilterStrategy<K, V> disabledTenantMessageFilter() {
      return consumerRecord -> false;
    }
  }
}
