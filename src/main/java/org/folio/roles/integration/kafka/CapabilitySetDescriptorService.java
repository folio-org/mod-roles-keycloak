package org.folio.roles.integration.kafka;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.Collectors.toLinkedHashMap;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.utils.CollectionUtils.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.repository.PermissionRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.spring.FolioExecutionContext;
import org.jetbrains.annotations.NotNull;
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
  private final PermissionRepository permissionRepository;

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

    var alreadyCreatedCapabilitySets = toStream(newCapabilitySetDescriptors)
      .collect(groupingBy(csd -> existingCapabilitySets.containsKey(csd.getName())));

    handleNewCapabilitySets(alreadyCreatedCapabilitySets.get(FALSE));
    handleUpdatedCapabilitySets(resourceEventType, alreadyCreatedCapabilitySets.get(TRUE), existingCapabilitySets);
    handleDeprecatedCapabilitySets(oldCapabilitySetDescriptors, newCapabilityNames);
  }

  /**
   * Raises version for existing capability sets by module id + application id.
   *
   * @param moduleId - module identifier
   * @param newApplicationId - new application identifier
   * @param oldApplicationId - old application identifier
   */
  @Transactional
  public void updateApplicationVersion(String moduleId, String newApplicationId, String oldApplicationId) {
    capabilitySetService.updateApplicationVersion(moduleId, newApplicationId, oldApplicationId);
  }

  /**
   * Raises version for existing capability sets by module name + application name.
   *
   * @param applicationName - application name
   * @param moduleName - module name
   * @param newApplicationId - new application identifier
   * @param newModuleId - new module identifier
   */
  @Transactional
  public void updateAppAndModuleVersionByAppAndModuleName(String applicationName, String moduleName,
    String newApplicationId, String newModuleId) {
    capabilitySetService.updateAppAndModuleVersionByAppAndModuleName(applicationName, moduleName, newApplicationId,
      newModuleId);
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
      log.debug("Prepared new capability set for creation: name = {}, permission = {}",
        capabilitySet.getName(), capabilitySet.getPermission());
    }

    var createdCapabilitySets = capabilitySetService.createAll(capabilitySets);
    for (var createdCapabilitySet : createdCapabilitySets) {
      var capabilityList = mapItems(createdCapabilitySet.getCapabilities(), capabilityMap::get);
      var extendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(createdCapabilitySet, capabilityList);
      updateParentCapabilitySetsByDummyCapabilities(createdCapabilitySet, extendedCapabilitySet);
      var event = CapabilitySetEvent.created(extendedCapabilitySet).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }

  private void updateParentCapabilitySetsByDummyCapabilities(CapabilitySet createdCapabilitySet,
    ExtendedCapabilitySet extendedCapabilitySet) {
    if (hasNoDummyCapabilities(extendedCapabilitySet)) {
      log.debug("No dummy capabilities found in capability set: {}", extendedCapabilitySet.getName());
      return;
    }

    var dummiesToAdd = toStream(extendedCapabilitySet.getCapabilityList())
      .filter(c -> isTrue(c.getDummyCapability()))
      .map(Capability::getId)
      .toList();
    var capSetPermission = createdCapabilitySet.getPermission();
    var parentCapSetsPermissions = permissionRepository.getAllParentPermissions(capSetPermission);
    parentCapSetsPermissions.remove(capSetPermission);

    var parentSets = capabilitySetService.findByPermissionNames(parentCapSetsPermissions);
    for (var cs : parentSets) {
      cs.getCapabilities().addAll(dummiesToAdd);
    }
    capabilitySetService.updateAll(parentSets);
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

  private List<Capability> resolveCapabilities(CapabilitySetDescriptor capabilitySetDescriptor) {
    detectAndLogCapabilityCollisionInSet(capabilitySetDescriptor);

    var requiredCapability = filterAndGetCapabilities(capabilitySetDescriptor);

    if (isEmpty(requiredCapability)) {
      log.warn("Capabilities are empty for capability set: name = {}", capabilitySetDescriptor.getName());
      return emptyList();
    }

    var capabilities = new ArrayList<Capability>();
    var alreadySavedCapabilities =
      capabilityService.findByNamesIncludeDummy(mapItems(requiredCapability, Capability::getName));
    var savedCapabilitiesByNames = alreadySavedCapabilities.stream().collect(toMap(Capability::getName, identity()));

    // create dummy for all not found capabilities in db ->
    for (var capabilityFromEvent : requiredCapability) {
      var capability = savedCapabilitiesByNames.get(capabilityFromEvent.getName());
      if (capability == null) {
        capability = createDummyCapabilityAndSendEvent(capabilitySetDescriptor, capabilityFromEvent);
      }
      capabilities.add(capability);
    }

    return capabilities;
  }

  private Map<String, CapabilitySet> getExistingCapabilitySetsByNames(Set<String> newCapabilityNames) {
    if (isEmpty(newCapabilityNames)) {
      return emptyMap();
    }

    return toStream(capabilitySetService.findByNames(newCapabilityNames))
      .collect(toLinkedHashMap(CapabilitySet::getName));
  }

  private Capability createDummyCapabilityAndSendEvent(CapabilitySetDescriptor capabilitySetDescriptor,
    Capability capabilityFromEvent) {
    log.warn("Capability is not found by name: {}, creating a dummy one", capabilityFromEvent.getName());
    capabilityFromEvent.setDummyCapability(TRUE);
    capabilityFromEvent.setApplicationId(capabilitySetDescriptor.getApplicationId());
    capabilityFromEvent.setModuleId(capabilitySetDescriptor.getModuleId());
    var capability = capabilityService.save(capabilityFromEvent);
    log.info("Created dummy capability with name: {}", capabilityFromEvent.getName());

    var event = CapabilityEvent.created(capability);
    applicationEventPublisher.publishEvent(event);
    return capability;
  }

  private static @NotNull List<Capability> filterAndGetCapabilities(CapabilitySetDescriptor capabilitySetDescriptor) {
    return capabilitySetDescriptor.getCapabilities()
      .stream()
      .collect(Collectors.toMap(
        Capability::getName,
        Function.identity(),
        (a, b) -> a, LinkedHashMap::new))
      .values()
      .stream()
      .toList();
  }

  private static void detectAndLogCapabilityCollisionInSet(CapabilitySetDescriptor capabilitySetDescriptor) {
    var duplicateMap = new HashMap<String, List<String>>();
    toStream(capabilitySetDescriptor.getCapabilities()).forEach(c -> {
      var values = duplicateMap.getOrDefault(c.getName(), new ArrayList<>());
      values.add(c.getPermission());
      duplicateMap.put(c.getName(), values);
    });
    duplicateMap.forEach((key, value) -> {
      if (value.size() > 1) {
        log.warn(
          "Capability collision detected: "
            + "CapabilitySet '{}' (permission: '{}') contains capability '{}' with multiple permissions: {}",
          capabilitySetDescriptor.getName(), capabilitySetDescriptor.getPermission(), key, value);
      }
    });
  }

  private static boolean hasNoDummyCapabilities(ExtendedCapabilitySet extendedCapabilitySet) {
    return toStream(extendedCapabilitySet.getCapabilityList()).noneMatch(c -> isTrue(c.getDummyCapability()));
  }
}
