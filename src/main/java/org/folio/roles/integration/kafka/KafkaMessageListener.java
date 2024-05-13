package org.folio.roles.integration.kafka;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final FolioModuleMetadata metadata;
  private final CapabilityKafkaEventHandler capabilityKafkaEventHandler;

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
    try (var ignored = new FolioExecutionContextSetter(metadata, Map.of(TENANT, List.of(resourceEvent.getTenant())))) {
      capabilityKafkaEventHandler.handleEvent(resourceEvent);
    }
  }
}
