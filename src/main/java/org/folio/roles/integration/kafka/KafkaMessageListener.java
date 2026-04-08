package org.folio.roles.integration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityReplacementsService;
import org.folio.roles.service.capability.UserPermissionsCacheEvictor;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.folio.spring.liquibase.LiquibaseMigrationLockService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final ExecutionContextBuilder executionContextBuilder;
  private final CapabilityKafkaEventHandler capabilityKafkaEventHandler;
  private final CapabilityReplacementsService capabilityReplacementsService;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final UserPermissionsCacheEvictor userPermissionsCacheEvictor;
  private final LiquibaseMigrationLockService liquibaseMigrationLockService;

  /**
   * Handles capability event.
   *
   * @param resourceEvent - capability {@link ResourceEvent} object
   */
  @KafkaListener(
    id = "capability-event-listener",
    containerFactory = "kafkaListenerContainerFactory",
    groupId = "#{folioKafkaProperties.listener['capability'].groupId}",
    topicPattern = "#{folioKafkaProperties.listener['capability'].topicPattern}",
    filter = "tenantAwareMessageFilter")
  public void handleCapabilityEvent(ResourceEvent resourceEvent) {
    try (
      var ignored = new FolioExecutionContextSetter(executionContextBuilder.buildContext(resourceEvent.getTenant()))) {
      try {
        checkLiquibaseMigrationRunning(resourceEvent);

        systemUserScopedExecutionService.executeSystemUserScoped(() -> {
          var capabilityReplacements = capabilityKafkaEventHandler.handleEvent(resourceEvent);
          capabilityReplacements.ifPresent(capabilityReplacementsService::processReplacements);
          return null;
        });
      } finally {
        userPermissionsCacheEvictor.evictUserPermissionsForCurrentTenant();
      }
    }
  }

  private void checkLiquibaseMigrationRunning(ResourceEvent resourceEvent) {
    if (liquibaseMigrationLockService.isMigrationRunning()) {
      log.warn("Liquibase migration in progress for tenant: {}", resourceEvent.getTenant());
      throw new LiquibaseMigrationException(
        "Liquibase migration is still running for tenant: " + resourceEvent.getTenant());
    }
  }
}
