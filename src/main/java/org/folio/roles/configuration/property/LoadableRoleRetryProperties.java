package org.folio.roles.configuration.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.loadable-role.retry")
public class LoadableRoleRetryProperties {

  @Min(value = 1, message = "Max attempts must be at least 1")
  private int maxAttempts;
  
  @Valid
  private Backoff backoff = new Backoff();

  @Data
  public static class Backoff {
    @Min(value = 1000, message = "Backoff delay must be at least 1000ms")
    private long delayMs;
  }
}
