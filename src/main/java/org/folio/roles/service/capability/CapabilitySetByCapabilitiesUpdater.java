package org.folio.roles.service.capability;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilitySetByCapabilitiesUpdater {

  private final CapabilitySetMapper capabilitySetMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final CapabilitySetService capabilitySetService;
  private final CapabilityService capabilityService;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Update capability set by capabilities and publish update event.
   *
   * @param capabilitiesToAdd - capabilities to add to capability set
   * @param capabilitySet - capability set to update
   */
  public void update(CapabilitySet capabilitySet, List<Capability> capabilitiesToAdd) {
    log.info("Update capability set by capabilities : capability set name - {},  capabilities - {}",
      capabilitySet.getName(),
      toStream(capabilitiesToAdd).map(Capability::getName).collect(Collectors.joining(", ")));
    var capabilities = capabilityService.findByCapabilitySetIdsIncludeDummy(Set.of(capabilitySet.getId()));
    var extendedCapabilitySet = capabilitySetMapper
      .toExtendedCapabilitySet(capabilitySet, capabilities);
    var updatedCapabilities = new LinkedHashSet<>(capabilities);
    updatedCapabilities.addAll(capabilitiesToAdd);
    var updatedExtendedCapabilitySet = capabilitySetMapper
      .toExtendedCapabilitySet(capabilitySet, toStream(updatedCapabilities).toList());

    var capabilityIdsToAdd = capabilitiesToAdd
      .stream()
      .map(Capability::getId)
      .toList();
    capabilitySetService.addCapabilitiesById(capabilitySet.getId(), capabilityIdsToAdd);
    var event = CapabilitySetEvent.updated(updatedExtendedCapabilitySet, extendedCapabilitySet);
    applicationEventPublisher.publishEvent(event.withContext(folioExecutionContext));
  }
}
