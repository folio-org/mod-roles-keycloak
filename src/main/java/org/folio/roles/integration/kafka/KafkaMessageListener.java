package org.folio.roles.integration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityReplacementsService;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final ExecutionContextBuilder executionContextBuilder;
  private final CapabilityKafkaEventHandler capabilityKafkaEventHandler;
  private final CapabilityReplacementsService capabilityReplacementsService;

  /**
   * Handles capability event.
   *
   * @param resourceEvent - capability {@link ResourceEvent} object
   */
  @KafkaListener(
    id = "capability-event-listener",
    containerFactory = "kafkaListenerContainerFactory",
    groupId = "#{folioKafkaProperties.listener['capability'].groupId}",
    topicPattern = "#{folioKafkaProperties.listener['capability'].topicPattern}")
  public void handleCapabilityEvent(ResourceEvent resourceEvent) {
    try (
      var ignored = new FolioExecutionContextSetter(executionContextBuilder.buildContext(resourceEvent.getTenant()))) {
      var capabilityReplacements = capabilityKafkaEventHandler.handleEvent(resourceEvent);
      capabilityReplacements.ifPresent(capabilityReplacementsService::processReplacements);
    }
  }
}
