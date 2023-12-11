package org.folio.roles.integration.kafka;

import static java.util.stream.Collectors.toList;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.permission.FolioPermissionService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final ObjectMapper objectMapper;
  private final FolioModuleMetadata metadata;
  private final CapabilityService capabilityService;
  private final FolioPermissionService folioPermissionService;
  private final CapabilityEventProcessor capabilityEventProcessor;
  private final CapabilitySetDescriptorService capabilitySetDescriptorService;

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
      var event = objectMapper.convertValue(resourceEvent.getNewValue(), CapabilityEvent.class);
      log.info("Capability event received for module: {}", event.getModuleId());
      var applicationId = event.getApplicationId();
      folioPermissionService.createIgnoringConflicts(getPermissions(event));
      var resultHolder = capabilityEventProcessor.process(event);
      capabilityService.createSafe(applicationId, resultHolder.capabilities());
      capabilitySetDescriptorService.createSafe(applicationId, resultHolder.capabilitySets());
    }
  }

  private static List<Permission> getPermissions(CapabilityEvent event) {
    return toStream(event.getResources())
      .map(FolioResource::getPermission)
      .filter(Objects::nonNull)
      .collect(toList());
  }
}
