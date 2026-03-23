package org.folio.roles.integration.mte.configuration;

import lombok.RequiredArgsConstructor;
import org.folio.common.utils.tls.HttpClientTlsUtils;
import org.folio.roles.integration.mte.TenantEntitlementsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the mgr-tenant-entitlements HTTP client.
 *
 * <p>Creates a dedicated {@link TenantEntitlementsClient} proxy backed by a {@link RestClient}
 * pointed at {@code application.mte.url}, independent of the shared Okapi HTTP service factory.
 * TLS is configured via {@link MteConfigurationProperties#getTls()}.</p>
 */
@Configuration
@RequiredArgsConstructor
public class MteConfiguration {

  private final MteConfigurationProperties properties;

  /**
   * Creates the {@link TenantEntitlementsClient} Spring {@code @HttpExchange} proxy.
   *
   * @param builder Spring auto-configured {@link RestClient.Builder} (prototype — fresh per injection)
   * @return proxy instance backed by a dedicated {@link RestClient} targeting MTE's base URL
   */
  @Bean
  public TenantEntitlementsClient tenantEntitlementsClient(RestClient.Builder builder) {
    return HttpClientTlsUtils.buildHttpServiceClient(
      builder, properties.getTls(), properties.getUrl(), TenantEntitlementsClient.class);
  }
}
