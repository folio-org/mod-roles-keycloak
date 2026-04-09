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
  public static class TenantFilterConfiguration {

    private final ModuleMetadata moduleMetadata;
    private final TenantFilter tenantFilter;

    public TenantFilterConfiguration(ModuleMetadata moduleMetadata, FolioKafkaProperties kafkaProperties) {
      this.moduleMetadata = moduleMetadata;
      this.tenantFilter = kafkaProperties.getFiltering().getTenantFilter();
    }

    @Bean
    public TenantEntitlementClient tenantEntitlementClient(HttpServiceProxyFactory factory) {
      return factory.createClient(TenantEntitlementClient.class);
    }

    @Bean
    public TenantEntitlementService tenantEntitlementService(TenantEntitlementClient tenantEntitlementClient) {
      return new TenantEntitlementService(moduleMetadata.getModuleId(), tenantEntitlementClient);
    }

    @Bean("tenantAwareMessageFilter")
    public <K, V extends ResourceEvent> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
      TenantEntitlementService tenantEntitlementService) {
      return new EnabledTenantMessageFilter<>(
        moduleMetadata.getModuleId(),
        tenantEntitlementService,
        tenantFilter.isIgnoreEmptyBatch(),
        tenantFilter.getTenantDisabledStrategy(),
        tenantFilter.getAllTenantsDisabledStrategy()
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
