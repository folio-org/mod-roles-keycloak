package org.folio.roles.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.Collectors.toLinkedHashMap;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;
import static org.folio.roles.utils.CollectionUtils.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilitySetDescriptorService {

  private final CapabilityService capabilityService;
  private final CapabilitySetMapper capabilitySetMapper;
  private final CapabilitySetService capabilitySetService;
  private final FolioExecutionContext folioExecutionContext;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Updates capabilities using given event type, old and new collection of capability set descriptors.
   *
   * @param resourceEventType - resource event type
   * @param newCapabilitySetDescriptors - list with new capability set descriptors
   * @param oldCapabilitySetDescriptors - value before a module has been upgraded or disabled
   */
  @Transactional
  public void update(ResourceEventType resourceEventType,
    List<CapabilitySetDescriptor> newCapabilitySetDescriptors,
    List<CapabilitySetDescriptor> oldCapabilitySetDescriptors) {

    log.info("Capability set(s) received: new = {}, old = {}",
      newCapabilitySetDescriptors.size(), oldCapabilitySetDescriptors.size());

    var newCapabilityNames = toSet(newCapabilitySetDescriptors, CapabilitySetDescriptor::getName);
    var existingCapabilitySets = getExistingCapabilitySetsByNames(newCapabilityNames);

    var groupedCapabilitySets = toStream(newCapabilitySetDescriptors)
      .collect(groupingBy(csd -> existingCapabilitySets.containsKey(csd.getName())));

    handleNewCapabilitySets(groupedCapabilitySets.get(Boolean.FALSE));
    handleUpdatedCapabilitySets(resourceEventType, groupedCapabilitySets.get(Boolean.TRUE), existingCapabilitySets);
    handleDeprecatedCapabilitySets(oldCapabilitySetDescriptors, newCapabilityNames);
  }

  private void handleNewCapabilitySets(List<CapabilitySetDescriptor> capabilitySetDescriptors) {
    if (isEmpty(capabilitySetDescriptors)) {
      return;
    }

    var capabilitySets = new ArrayList<CapabilitySet>();
    var capabilityMap = new HashMap<UUID, Capability>();
    for (var setDescriptor : capabilitySetDescriptors) {
      var capabilityList = resolveCapabilities(setDescriptor);
      capabilityList.forEach(capability -> capabilityMap.put(capability.getId(), capability));
      var capabilitySet = capabilitySetMapper.convert(setDescriptor);
      capabilitySet.setCapabilities(mapItems(capabilityList, Capability::getId));
      capabilitySets.add(capabilitySet);
    }

    var createdCapabilitySets = capabilitySetService.createAll(capabilitySets);
    for (var createdCapabilitySet : createdCapabilitySets) {
      var capabilityList = mapItems(createdCapabilitySet.getCapabilities(), capabilityMap::get);
      var extendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(createdCapabilitySet, capabilityList);
      var event = CapabilitySetEvent.created(extendedCapabilitySet).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }

  private void handleUpdatedCapabilitySets(ResourceEventType type, List<CapabilitySetDescriptor> setDescriptors,
    Map<String, CapabilitySet> capabilitySetsByName) {
    if (isEmpty(setDescriptors)) {
      return;
    }

    var capabilitySets = new ArrayList<CapabilitySet>();
    var oldCapabilitySets = new HashMap<UUID, ExtendedCapabilitySet>();
    var capabilityMap = new HashMap<UUID, Capability>();

    for (var updatedSetDesc : setDescriptors) {
      var capabilityList = resolveCapabilities(updatedSetDesc);
      capabilityList.forEach(capability -> capabilityMap.put(capability.getId(), capability));

      var existingSet = capabilitySetsByName.get(updatedSetDesc.getName());
      var updatedCapabilitySet = capabilitySetMapper.convert(updatedSetDesc);
      updatedCapabilitySet.setId(existingSet.getId());
      updatedCapabilitySet.setCapabilities(mapItems(capabilityList, Capability::getId));
      capabilitySets.add(updatedCapabilitySet);

      var oldSetCapabilities = capabilityService.findByIds(existingSet.getCapabilities());
      var existingExtendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(existingSet, oldSetCapabilities);
      oldCapabilitySets.put(existingSet.getId(), existingExtendedCapabilitySet);
    }

    var updatedCapabilitySets = capabilitySetService.createAll(capabilitySets);
    if (type == CREATE) {
      log.warn("Duplicated capability sets has been updated: {}",
        mapItems(oldCapabilitySets.values(), CapabilitySet::getName));
    }

    for (var updatedCapabilitySet : updatedCapabilitySets) {
      var updatedCapabilitySetId = updatedCapabilitySet.getId();
      var capabilityList = mapItems(updatedCapabilitySet.getCapabilities(), capabilityMap::get);
      var extendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(updatedCapabilitySet, capabilityList);
      var event = CapabilitySetEvent.updated(extendedCapabilitySet, oldCapabilitySets.get(updatedCapabilitySetId));
      applicationEventPublisher.publishEvent(event.withContext(folioExecutionContext));
    }
  }

  private void handleDeprecatedCapabilitySets(
    List<CapabilitySetDescriptor> oldSetDescriptors, Set<String> capabilitySetNames) {
    var deprecatedCapabilitySetNames = toStream(oldSetDescriptors)
      .map(CapabilitySetDescriptor::getName)
      .filter(not(capabilitySetNames::contains))
      .collect(Collectors.toSet());

    if (isEmpty(deprecatedCapabilitySetNames)) {
      return;
    }

    var deprecatedCapabilitySets = capabilitySetService.findByNames(deprecatedCapabilitySetNames);
    for (var deprecatedCapabilitySet : deprecatedCapabilitySets) {
      var extendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(deprecatedCapabilitySet, emptyList());
      var event = CapabilitySetEvent.deleted(extendedCapabilitySet).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }

  private static Stream<String> getRequiredCapabilityNames(CapabilitySetDescriptor descriptor) {
    var capabilities = descriptor.getCapabilities();
    if (MapUtils.isEmpty(capabilities)) {
      return Stream.empty();
    }

    return capabilities.entrySet().stream()
      .flatMap(entry -> getCapabilityNames(descriptor, entry))
      .distinct();
  }

  private static Stream<String> getCapabilityNames(CapabilitySetDescriptor descriptor,
    Entry<String, List<CapabilityAction>> entry) {
    var resource = entry.getKey();
    var actions = entry.getValue();
    if (CollectionUtils.isEmpty(actions)) {
      log.warn("Capability set resource actions are empty: capabilitySet = {}, resource = {}",
        () -> getCapabilityName(descriptor.getResource(), descriptor.getAction()), () -> resource);
      return Stream.empty();
    }

    return toStream(actions).map(action -> getCapabilityName(resource, action));
  }

  private List<Capability> resolveCapabilities(CapabilitySetDescriptor capabilitySetDescriptor) {
    var requiredCapabilityNames = getRequiredCapabilityNames(capabilitySetDescriptor).toList();
    if (isEmpty(requiredCapabilityNames)) {
      log.warn("Capabilities are empty for capability set: name = {}", capabilitySetDescriptor.getName());
      return emptyList();
    }

    var capabilityIds = new ArrayList<Capability>();
    var capabilities = capabilityService.findByNames(requiredCapabilityNames);
    var capabilityIdsByNames = capabilities.stream().collect(toMap(Capability::getName, Function.identity()));
    for (var capabilityName : requiredCapabilityNames) {
      var capabilityId = capabilityIdsByNames.get(capabilityName);
      if (capabilityId == null) {
        log.warn("Capability id is not found by capability name: {}", capabilityName);
        continue;
      }

      capabilityIds.add(capabilityId);
    }

    return capabilityIds;
  }

  private Map<String, CapabilitySet> getExistingCapabilitySetsByNames(Set<String> newCapabilityNames) {
    if (isEmpty(newCapabilityNames)) {
      return emptyMap();
    }

    return toStream(capabilitySetService.findByNames(newCapabilityNames))
      .collect(toLinkedHashMap(CapabilitySet::getName));
  }
}
