package org.folio.roles.integration.mte.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the mgr-tenant-entitlements HTTP client.
 */
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "application.mte")
public class MteConfigurationProperties {

  /**
   * MTE service base URL.
   */
  @NotBlank
  private String url;

  /**
   * TLS configuration for MTE communication.
   */
  private TlsProperties tls;
}
