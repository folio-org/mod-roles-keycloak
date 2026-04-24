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
 *
 * <p>A fresh {@link RestClient.Builder} (via {@link RestClient#builder()}) is used intentionally
 * to avoid inheriting the shared {@code EnrichUrlAndHeadersInterceptor} from
 * {@code HttpServiceClientConfiguration}, which rewrites the URL base using the Okapi URL from
 * {@code FolioExecutionContext} and would corrupt MTE's own base URL.</p>
 */
@Configuration
@RequiredArgsConstructor
public class MteConfiguration {

  private final MteConfigurationProperties properties;

  /**
   * Creates the {@link TenantEntitlementsClient} Spring {@code @HttpExchange} proxy.
   *
   * <p>Uses {@link RestClient#builder()} (a new, interceptor-free builder) rather than the
   * Spring-injected shared builder, to prevent the Okapi URL-rewriting interceptor from
   * corrupting MTE requests.</p>
   *
   * @return proxy instance backed by a dedicated {@link RestClient} targeting MTE's base URL
   */
  @Bean
  public TenantEntitlementsClient tenantEntitlementsClient() {
    return HttpClientTlsUtils.buildHttpServiceClient(
      RestClient.builder(), properties.getTls(), properties.getUrl(), TenantEntitlementsClient.class);
  }
}
