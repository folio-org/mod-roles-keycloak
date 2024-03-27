package org.folio.roles.integration.keyclock.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.folio.roles.integration.keyclock.configuration.properties.KeycloakTlsProperties;
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
  private KeycloakTlsProperties tls;

  @Data
  public static class Login {
    private String clientNameSuffix;
  }
}
