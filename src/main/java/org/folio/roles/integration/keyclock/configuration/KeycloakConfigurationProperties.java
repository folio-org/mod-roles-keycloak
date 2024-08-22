package org.folio.roles.integration.keyclock.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak properties from application.yaml configuration.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakConfigurationProperties {

  /**
   * Keycloak client identifier.
   */
  private String clientId;

  /**
   * Keycloak service base URL.
   */
  private String baseUrl;

  /**
   * Authentication grant type.
   */
  private String grantType;

  /**
   * Properties object with an information about login client in Keycloak.
   */
  private Login login;

  /**
   * Properties object with an information about TLS configuration for Keycloak communication.
   */
  private TlsProperties tls;

  /**
   * Keycloak configuration for permission migration.
   */
  private MigrationProperties migration;

  @Data
  public static class Login {

    private String clientNameSuffix;
  }

  @Data
  public static class MigrationProperties {

    /**
     * Users batch size for users migration.
     */
    private int usersBatchSize = 100;
  }
}
