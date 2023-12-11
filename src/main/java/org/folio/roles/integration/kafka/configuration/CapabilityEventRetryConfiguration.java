package org.folio.roles.integration.kafka.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties("application.retry.capability-event")
public class CapabilityEventRetryConfiguration {

  /**
   * Retry delay for system user creation.
   *
   * <p>This property is required to resolve installation request for mod-users</p>
   */
  @NotNull
  private Duration retryDelay = Duration.ofMillis(250);

  /**
   * A number for Retry attempts for system user creation.
   *
   * <p>This property is required to resolve installation request for mod-users</p>
   */
  @NotNull
  @Positive
  private long retryAttempts = Long.MAX_VALUE;
}
