package org.folio.roles.integration.kafka;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetByCapabilitiesUpdater;
import org.folio.roles.service.capability.CapabilitySetService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilitySetByDummyUpdater {

  private final CapabilitySetEntityMapper capabilitySetEntityMapper;
  private final CapabilitySetService capabilitySetService;
  private final CapabilityService capabilityService;
  private final CapabilitySetByCapabilitiesUpdater capabilitySetByCapabilitiesUpdater;

  /**
   *  In case if there is a capability set with the same name as dummy capability
   *  add its capabilities to capability sets with dummy capability.
   *
   * @param dummyCapabilitiesNames - dummy capabilities names
   */
  public void update(List<String> dummyCapabilitiesNames) {
    for (var dummyCapabilityName : dummyCapabilitiesNames) {
      var capabilitySetOpt = capabilitySetService.findByName(dummyCapabilityName);
      capabilitySetOpt.ifPresent(capabilitySet -> {
        log.info("Found capability set by dummy name {}", dummyCapabilityName);
        var capabilitiesToAdd = capabilityService.findByCapabilitySetIdsIncludeDummy(Set.of(capabilitySet.getId()))
          .stream()
          .filter(capability -> !StringUtils.equals(capability.getName(), dummyCapabilityName))
          .toList();
        var relatedCapabilitySets = getRelatedCapabilitySets(dummyCapabilityName);
        for (var relatedCapabilitySet : relatedCapabilitySets) {
          if (!capabilitySet.getId().equals(relatedCapabilitySet.getId())) {
            log.info("Add capabilities from {} to related set {}",
              dummyCapabilityName, relatedCapabilitySet.getName());
            capabilitySetByCapabilitiesUpdater
              .updateCapabilitySetByCapabilities(capabilitiesToAdd, relatedCapabilitySet);
          }
        }
      });
    }
  }

  private List<CapabilitySet> getRelatedCapabilitySets(String dummyCapabilityName) {
    var retrievedNames = new HashSet<String>();
    var queue = new LinkedList<String>();
    queue.add(dummyCapabilityName);
    var capabilitySets = new HashSet<CapabilitySetEntity>();
    while (!queue.isEmpty()) {
      var capabilitySetCapabilityName = queue.poll();
      if (!retrievedNames.contains(capabilitySetCapabilityName)) {
        var capabilitySetsByCapability = capabilitySetService.findByCapabilityName(capabilitySetCapabilityName);
        capabilitySetsByCapability.forEach(capabilitySetEntity -> queue.add(capabilitySetEntity.getName()));
        capabilitySets.addAll(capabilitySetsByCapability);
        retrievedNames.add(capabilitySetCapabilityName);
      }
    }
    return capabilitySets
      .stream()
      .map(capabilitySetEntityMapper::convert)
      .toList();
  }
}
