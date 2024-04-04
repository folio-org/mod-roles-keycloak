package org.folio.roles.configuration;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * A configuration class that sets up JPA auditing in an application.
 */
@Configuration
@RequiredArgsConstructor
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider")
public class JpaAuditingConfig {

  private final FolioExecutionContext context;

  /**
   * Bean definition for AuditorAware instance.
   *
   * @return the auditor aware instance.
   */
  @Bean
  public AuditorAware<UUID> auditorAware() {
    return () -> Optional.ofNullable(context.getUserId());
  }

  /**
   * Bean definition for DateTimeProvider instance.
   *
   * @return the date time provider.
   */
  @Bean
  public DateTimeProvider dateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }
}
