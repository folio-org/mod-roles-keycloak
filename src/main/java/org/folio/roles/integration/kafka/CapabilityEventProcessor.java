package org.folio.roles.integration.kafka;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;
import static org.folio.roles.utils.CollectionUtils.union;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.common.utils.permission.PermissionUtils;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilityType;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.service.permission.FolioPermissionService;
import org.folio.roles.service.permission.PermissionOverrider;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilityEventProcessor {

  private final FolioPermissionService folioPermissionService;
  private final PermissionOverrider permissionOverrider;

  /**
   * Creates a {@link CapabilityResultHolder} with {@link Capability} and {@link CapabilitySetDescriptor} values.
   *
   * @param event - {@link CapabilityEvent} object to process
   * @return {@link CapabilityResultHolder} with created {@link Capability} and {@link CapabilitySetDescriptor} objects
   */
  public CapabilityResultHolder process(CapabilityEvent event) {
    if (event == null || isEmpty(event.getResources())) {
      return new CapabilityResultHolder(emptyList(), emptyList());
    }

    var folioResources = event.getResources();
    return processModuleResources(event, folioResources);
  }

  /**
   * Here, the capabilities will be created for all permissions, even those that are
   * so-called "PermissionSets" and include "SubPermissions."
   * It is needed to support "FOLIO PermissionSets" using plain Eureka CapabilitySets.
   * The Capabilities created to reflect the CapabilitySets are called "Technical Capabilities" and do not refer
   * to any actual permissions/access grants that can be available to users.
   * Such Capabilities can be distinguished by the following criteria:
   * the end-points list of the capability must be empty,
   * and the capability name must be equal to the capability set name that it reflects.
   * Both backend and UI modules are processed in the same way.
   */
  private CapabilityResultHolder processModuleResources(CapabilityEvent event, List<FolioResource> resources) {
    var grouped = groupByHavingSubPermissions(resources);
    var capabilities = mapItems(resources, res -> createCapability(event, res));
    var capabilitySetDescriptors =
      mapItems(grouped.get(TRUE), res -> createCapabilitySetDescriptor(event, res));
    return toCapabilityResultHolder(capabilities, capabilitySetDescriptors);
  }

  private Optional<CapabilitySetDescriptor> createCapabilitySetDescriptor(
    CapabilityEvent event, FolioResource resource) {
    var folioPermission = resource.getPermission().getPermissionName();
    var permissionMappingOverrides = permissionOverrider.getPermissionMappings();
    return Optional.of(extractPermissionData(folioPermission, permissionMappingOverrides))
      .filter(CapabilityEventProcessor::hasRequiredFields)
      .map(raw -> createCapabilitySetDescriptor(event, resource, raw));
  }

  private CapabilitySetDescriptor createCapabilitySetDescriptor(
    CapabilityEvent event, FolioResource res, PermissionData permissionData) {
    var permission = res.getPermission();
    /*
     * The SubPermissions are handled equally for both module types: MODULE and UI_MODULE.
     * It is needed to support cases when a PermissionSet defined in BE modules are used in UI modules.
     * see https://folio-org.atlassian.net/browse/MODROLESKC-240
     */
    var subPermissions =  union(permission.getSubPermissions(), List.of(permission.getPermissionName()));
    var subPermissionsExpanded = folioPermissionService.expandPermissionNames(subPermissions);

    var capabilities = subPermissionsExpanded.stream()
      .map(Permission::getPermissionName)
      .map(permissionName -> extractPermissionData(permissionName, permissionOverrider.getPermissionMappings()))
      .filter(CapabilityEventProcessor::hasRequiredFields)
      .map(data -> createCapability(event, res, data))
      .collect(groupingBy(Capability::getResource, TreeMap::new, mapping(Capability::getAction, toList())));

    return new CapabilitySetDescriptor()
      .name(getCapabilityName(permissionData))
      .type(CapabilityType.fromValue(permissionData.getType().getValue()))
      .action(CapabilityAction.fromValue(permissionData.getAction().getValue()))
      .resource(permissionData.getResource())
      .description(permission.getDescription())
      .moduleId(event.getModuleId())
      .applicationId(event.getApplicationId())
      .permission(permission.getPermissionName())
      .capabilities(capabilities);
  }

  public static PermissionData extractPermissionData(String permissionName,
    Map<String, PermissionData> permissionMappings) {

    var permission = permissionMappings.get(permissionName);
    return permission != null ? permission : PermissionUtils.extractPermissionData(permissionName);
  }

  private Optional<Capability> createCapability(CapabilityEvent event, FolioResource resource) {
    var folioPermission = resource.getPermission().getPermissionName();
    var permissionMappingOverrides = permissionOverrider.getPermissionMappings();
    return Optional.of(extractPermissionData(folioPermission, permissionMappingOverrides))
      .filter(CapabilityEventProcessor::hasRequiredFields)
      .map(data -> createCapability(event, resource, data));
  }

  private static Capability createCapability(CapabilityEvent event, FolioResource resource,
    PermissionData permissionData) {
    var permission = resource.getPermission();
    return new Capability()
      .name(getCapabilityName(permissionData))
      .type(CapabilityType.fromValue(permissionData.getType().getValue()))
      .action(CapabilityAction.fromValue(permissionData.getAction().getValue()))
      .resource(permissionData.getResource())
      .moduleId(event.getModuleId())
      .description(permission.getDescription())
      .permission(permission.getPermissionName())
      .applicationId(event.getApplicationId())
      .endpoints(emptyIfNull(resource.getEndpoints()));
  }

  private static boolean hasRequiredFields(org.folio.common.utils.permission.model.PermissionData permissionData) {
    if (PermissionUtils.hasRequiredFields(permissionData)) {
      return true;
    }

    log.warn("Capability cannot be resolved: there is no at least one of required field: {}", permissionData);
    return false;
  }

  private static Map<Boolean, List<FolioResource>> groupByHavingSubPermissions(List<FolioResource> folioResources) {
    return folioResources.stream().collect(groupingBy(CapabilityEventProcessor::hasSubPermissions));
  }

  private static boolean hasSubPermissions(FolioResource resource) {
    return isNotEmpty(resource.getPermission().getSubPermissions());
  }

  private static <T, R> List<R> mapItems(Collection<T> nullableCollection, Function<T, Optional<R>> mapper) {
    return toStream(nullableCollection)
      .map(mapper)
      .flatMap(Optional::stream)
      .collect(toList());
  }

  private static <T, V> List<T> cleanDuplicates(List<T> collection, Function<T, V> identifiersMapper) {
    var resultList = new ArrayList<T>();
    var visitedIdentifiers = new HashMap<V, Pair<T, Integer>>();
    for (int i = 0; i < collection.size(); i++) {
      var element = collection.get(i);
      var identifier = identifiersMapper.apply(element);
      var foundElementPair = visitedIdentifiers.get(identifier);
      if (foundElementPair != null) {
        var mergeResult = mergeCapabilities(element, foundElementPair.getLeft());
        if (mergeResult.isEmpty()) {
          var elementName = element.getClass().getSimpleName();
          log.info("Duplicated {} name found: resource = {}", uncapitalize(elementName), identifier);
          continue;
        }

        resultList.set(foundElementPair.getRight(), mergeResult.get());
        continue;
      }

      resultList.add(element);
      visitedIdentifiers.put(identifier, Pair.of(element, i));
    }

    return resultList;
  }

  private static <T> Optional<T> mergeCapabilities(T object, T foundElement) {
    if (!(object instanceof Capability capability)) {
      return Optional.empty();
    }

    var endpoints = capability.getEndpoints();
    var foundCapability = (Capability) foundElement;
    var foundEndpoints = foundCapability.getEndpoints();
    if (endpoints.size() != 1 || foundEndpoints.size() != 1) {
      return Optional.empty();
    }

    var endpoint = endpoints.get(0);
    var foundEndpoint = foundEndpoints.get(0);
    if (isNotPossibleToMerge(endpoint, foundEndpoint)) {
      return Optional.empty();
    }

    var resultCapability = new Capability();
    BeanUtils.copyProperties(foundCapability, resultCapability);
    var permission = capability.getPermission();
    var permissionName = permission.endsWith(".put") ? permission : foundCapability.getPermission();
    //noinspection unchecked
    return Optional.of((T) resultCapability
      .endpoints(List.of(foundEndpoint, endpoint))
      .permission(permissionName));
  }

  private static boolean isNotPossibleToMerge(Endpoint endpoint, Endpoint foundEndpoint) {
    var path = endpoint.getPath();
    var foundPath = foundEndpoint.getPath();
    return !(Objects.equals(path, foundPath) && hasEditHttpMethod(endpoint) && hasEditHttpMethod(foundEndpoint));
  }

  private static boolean hasEditHttpMethod(Endpoint endpoint) {
    var method = endpoint.getMethod();
    return method == HttpMethod.PUT || method == HttpMethod.PATCH;
  }

  private static CapabilityResultHolder toCapabilityResultHolder(List<Capability> capabilities,
    List<CapabilitySetDescriptor> capabilitySetDescriptors) {
    var distinctCapabilities = cleanDuplicates(capabilities, Capability::getName);
    var distinctCapabilitySetDescriptors = cleanDuplicates(capabilitySetDescriptors, CapabilitySetDescriptor::getName);
    return new CapabilityResultHolder(distinctCapabilities, distinctCapabilitySetDescriptors);
  }
}
