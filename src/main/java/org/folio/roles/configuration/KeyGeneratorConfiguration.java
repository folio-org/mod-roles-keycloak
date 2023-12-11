package org.folio.roles.configuration;

import lombok.AllArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Key generators configuration responsible for defining custom generators.
 */
@Configuration
@AllArgsConstructor
public class KeyGeneratorConfiguration {

  private final FolioExecutionContext folioExecutionContext;

  /**
   * Creates a bean for defining {@link KeyGenerator} to generate keys. One cache value for each tenant.
   *
   * @return {@link KeyGenerator} bean that generates key based on the tenant ID.
   */
  @Bean
  public KeyGenerator clientIdByTenantKeyGenerator() {
    return (target, method, params) -> folioExecutionContext.getTenantId();
  }
}
