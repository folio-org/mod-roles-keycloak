package org.folio.roles.integration.kafka;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.model.CapabilityReplacements;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.capability.CapabilityReplacementsService;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.permission.FolioPermissionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilityKafkaEventHandler {

  private final ObjectMapper objectMapper;
  private final CapabilityService capabilityService;
  private final FolioPermissionService folioPermissionService;
  private final CapabilityEventProcessor capabilityEventProcessor;
  private final CapabilitySetDescriptorService capabilitySetDescriptorService;
  private final CapabilityReplacementsService capabilityReplacementsService;

  /**
   * Handles resource event containing created, updated, or deprecated capabilities and capability sets.
   *
   * @param resourceEvent - resource event from message bus
   */
  @Transactional
  public Optional<CapabilityReplacements> handleEvent(ResourceEvent resourceEvent) {
    var eventType = resourceEvent.getType();
    var newValue = objectMapper.convertValue(resourceEvent.getNewValue(), CapabilityEvent.class);
    var oldValue = objectMapper.convertValue(resourceEvent.getOldValue(), CapabilityEvent.class);
    var moduleId = newValue != null ? newValue.getModuleId() : oldValue.getModuleId();
    log.info("Capability event received: moduleId = {}, type = {}", moduleId, eventType);

    if (newValue != null && oldValue != null && isApplicationVersionUpgradeEvent(newValue, oldValue)) {
      var newApplicationId = newValue.getApplicationId();
      var oldApplicationId = oldValue.getApplicationId();
      capabilityService.updateApplicationVersion(moduleId, newApplicationId, oldApplicationId);
      capabilitySetDescriptorService.updateApplicationVersion(moduleId, newApplicationId, oldApplicationId);

      return Optional.empty();
    }

    final var capabilityReplacements = capabilityReplacementsService.deduceReplacements(newValue);

    var orh = capabilityEventProcessor.process(oldValue);

    folioPermissionService.update(getPermissions(newValue), getPermissions(oldValue));
    var nrh = capabilityEventProcessor.process(newValue);

    capabilityService.update(eventType, nrh.capabilities(), orh.capabilities());
    capabilitySetDescriptorService.update(eventType, nrh.capabilitySets(), orh.capabilitySets());

    return capabilityReplacements;
  }

  private static List<Permission> getPermissions(CapabilityEvent eventPayload) {
    if (eventPayload == null) {
      return Collections.emptyList();
    }

    return toStream(eventPayload.getResources())
      .map(FolioResource::getPermission)
      .filter(Objects::nonNull)
      .toList();
  }

  private static boolean isApplicationVersionUpgradeEvent(CapabilityEvent newValue, CapabilityEvent oldValue) {
    return Objects.equals(newValue.getModuleId(), oldValue.getModuleId())
      && isEmpty(newValue.getResources())
      && isEmpty(oldValue.getResources());
  }
}
