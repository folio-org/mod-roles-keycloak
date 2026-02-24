package org.folio.roles.integration.keyclock.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Keycloak properties from application.yaml configuration.
 */
@Getter
@Setter
@Validated
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


  /**
   * Containing retry configuration for Keycloak communication.
   **/
  @Valid
  private Retry retry;
  
  /**
   * Concurrency configuration for parallel Keycloak operations.
   */
  @Valid
  private Concurrency concurrency;

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

  @Data
  public static class Retry {

    /**
     * The maximum number of retry attempts.
     */
    @Min(1)
    @Max(10)
    private int maxAttempts;

    @Valid
    private Backoff backoff;
  }

  @Data
  public static class Concurrency {
  
    /**
     * Maximum number of threads in the shared pool used for parallel Keycloak calls.
     */
    @Min(1)
    @Max(200)
    private int threadPoolSize = 20;
  }
  
  @Data
  public static class Backoff {

    /**
     * The initial delay in milliseconds before retrying.
     */
    @Min(1000)
    @Max(60000)
    private long delayMs;
  }
}
