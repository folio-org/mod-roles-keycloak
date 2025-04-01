package org.folio.roles.integration.kafka;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilitySetByDummyUpdater {

  private final CapabilitySetEntityMapper capabilitySetEntityMapper;
  private final CapabilitySetService capabilitySetService;
  private final CapabilityService capabilityService;
  private final CapabilitySetMapper capabilitySetMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final ApplicationEventPublisher applicationEventPublisher;

  public void update(List<String> dummyCapabilitiesNames) {
    for (var dummyCapabilityName : dummyCapabilitiesNames) {
      var capabilitySetOpt = capabilitySetService.findByName(dummyCapabilityName);
      capabilitySetOpt.ifPresent(capabilitySet -> {
        log.info("Found capability set by dummy name {}", dummyCapabilityName);
        var capabilitiesToAdd = capabilityService.findByCapabilitySetIds(Set.of(capabilitySet.getId()));
        var relatedCapabilitySets = getRelatedCapabilitySets(dummyCapabilityName);
        for (var relatedCapabilitySet : relatedCapabilitySets) {
          if (!capabilitySet.getId().equals(relatedCapabilitySet.getId())) {
            log.info("Add capabilities from {} to related set {}",
              dummyCapabilityName, relatedCapabilitySet.getName());
            updateRelatedCapabilitySet(capabilitiesToAdd, relatedCapabilitySet);
          }
        }
      });
    }
  }

  private List<CapabilitySet> getRelatedCapabilitySets(String dummyCapabilityName) {
    var checkedCapabilityNames = new HashSet<String>();
    var queue = new LinkedList<String>();
    queue.add(dummyCapabilityName);
    var capabilitySets = new HashSet<CapabilitySetEntity>();
    while (queue.size() > 0) {
      var dummy = queue.poll();
      if (!checkedCapabilityNames.contains(dummy)) {
        var capabilitySetsByCapability = capabilitySetService.findByCapabilityName(dummy);
        capabilitySetsByCapability.forEach(capabilitySetEntity -> queue.add(capabilitySetEntity.getName()));
        capabilitySets.addAll(capabilitySetsByCapability);
        checkedCapabilityNames.add(dummy);
      }
    }
    return capabilitySets
      .stream()
      .map(capabilitySetEntityMapper::convert)
      .toList();
  }

  private void updateRelatedCapabilitySet(List<Capability> capabilitiesToAdd, CapabilitySet relatedCapabilitySet) {
    var relatedSetCapabilities = capabilityService.findByCapabilitySetIds(Set.of(relatedCapabilitySet.getId()));
    var relatedExtendedCapabilitySet = capabilitySetMapper
      .toExtendedCapabilitySet(relatedCapabilitySet, relatedSetCapabilities);
    var updatedCapabilities = new LinkedHashSet<>(relatedSetCapabilities);
    updatedCapabilities.addAll(capabilitiesToAdd);
    var updatedRelatedExtendedCapabilitySet = capabilitySetMapper
      .toExtendedCapabilitySet(relatedCapabilitySet, updatedCapabilities.stream().toList());

    var capabilityIdsToAdd = capabilitiesToAdd
      .stream()
      .map(Capability::getId)
      .toList();
    capabilitySetService.addCapabilitiesById(capabilityIdsToAdd, relatedCapabilitySet.getId());
    var event = CapabilitySetEvent.updated(updatedRelatedExtendedCapabilitySet, relatedExtendedCapabilitySet);
    applicationEventPublisher.publishEvent(event.withContext(folioExecutionContext));
  }
}
