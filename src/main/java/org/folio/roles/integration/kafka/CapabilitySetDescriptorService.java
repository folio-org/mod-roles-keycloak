package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilitySetDescriptorService {

  private final CapabilityService capabilityService;
  private final CapabilitySetMapper capabilitySetMapper;
  private final CapabilitySetService capabilitySetService;

  @Transactional
  public void createSafe(String applicationId, List<CapabilitySetDescriptor> capabilitySetDescriptors) {
    if (isEmpty(capabilitySetDescriptors)) {
      log.debug("Nothing to create. Capabilities collection is empty in resource event");
      return;
    }

    log.info("{} capability set(s) received", capabilitySetDescriptors.size());

    var capabilitySets = new ArrayList<CapabilitySet>();
    var requiredCapabilityNames = new HashMap<String, List<String>>();
    for (var capabilitySetDescriptor : capabilitySetDescriptors) {
      var capabilitySet = capabilitySetMapper.convert(applicationId, capabilitySetDescriptor);
      var capabilitySetName = capabilitySet.getName();
      if (requiredCapabilityNames.containsKey(capabilitySetName)) {
        log.warn("Duplicated capability found: name = {}", capabilitySetName);
      } else if (capabilitySetService.existsByName(capabilitySetName)) {
        log.warn("CapabilitySet by name already exists: name = {}", capabilitySetName);
      } else {
        capabilitySets.add(capabilitySet);
        requiredCapabilityNames.put(capabilitySetName, getRequiredCapabilityNames(capabilitySetDescriptor));
      }
    }

    createCapabilitySets(requiredCapabilityNames, capabilitySets);
  }

  private void createCapabilitySets(Map<String, List<String>> capabilityNamesMap, List<CapabilitySet> capabilitySets) {
    var allCapabilityNames = getAllCapabilityNames(capabilityNamesMap);
    var existingCapabilityIdsMap = getExistingCapabilitiesMap(allCapabilityNames);
    for (var capabilitySet : capabilitySets) {
      createCapabilitySet(capabilityNamesMap, capabilitySet, existingCapabilityIdsMap);
    }
  }

  private void createCapabilitySet(Map<String, List<String>> capabilityNamesMap, CapabilitySet capabilitySet,
    Map<String, UUID> existingCapabilityIdsMap) {
    var capabilitySetName = capabilitySet.getName();

    var capabilityNames = capabilityNamesMap.get(capabilitySetName);
    if (CollectionUtils.isEmpty(capabilityNames)) {
      log.warn("Capabilities are empty for capability set: name = {}", capabilitySetName);
      return;
    }

    var capabilityIds = getCapabilityIds(capabilityNames, existingCapabilityIdsMap);
    if (CollectionUtils.isEmpty(capabilityIds)) {
      return;
    }

    capabilitySet.setCapabilities(capabilityIds);
    capabilitySetService.create(capabilitySet);
  }

  private Map<String, UUID> getExistingCapabilitiesMap(List<String> allCapabilityNames) {
    if (CollectionUtils.isEmpty(allCapabilityNames)) {
      return emptyMap();
    }

    var capabilities = capabilityService.findByNames(allCapabilityNames);
    return capabilities.stream().collect(toMap(Capability::getName, Capability::getId, (v1, v2) -> v2));
  }

  private List<UUID> getCapabilityIds(Collection<String> capabilityNames, Map<String, UUID> capabilityIdsMap) {
    return capabilityNames.stream()
      .map(capabilityName -> getCapabilityId(capabilityIdsMap, capabilityName))
      .flatMap(Optional::stream)
      .collect(toList());
  }

  private static List<String> getAllCapabilityNames(Map<String, List<String>> requiredCapabilityNames) {
    return requiredCapabilityNames.values().stream()
      .flatMap(Collection::stream)
      .collect(toList());
  }

  private Optional<UUID> getCapabilityId(Map<String, UUID> existingCapabilityIdsMap, String capabilityName) {
    var value = existingCapabilityIdsMap.get(capabilityName);
    if (value == null) {
      log.warn("Capability id is not found by capability name: {}", capabilityName);
      return Optional.empty();
    }

    return Optional.of(value);
  }

  private static List<String> getRequiredCapabilityNames(CapabilitySetDescriptor descriptor) {
    var capabilities = descriptor.getCapabilities();
    if (MapUtils.isEmpty(capabilities)) {
      return emptyList();
    }

    return capabilities.entrySet().stream()
      .map(entry -> getCapabilityNames(descriptor, entry))
      .flatMap(Collection::stream)
      .distinct()
      .collect(toList());
  }

  private static Collection<String> getCapabilityNames(CapabilitySetDescriptor descriptor,
    Entry<String, List<CapabilityAction>> entry) {
    var resource = entry.getKey();
    var actions = entry.getValue();
    if (CollectionUtils.isEmpty(actions)) {
      log.warn("Capability set resource actions are empty: capabilitySet = {}, resource = {}",
        () -> getCapabilityName(descriptor.getResource(), descriptor.getAction()), () -> resource);
      return emptyList();
    }

    return mapItems(actions, action -> getCapabilityName(resource, action));
  }
}
