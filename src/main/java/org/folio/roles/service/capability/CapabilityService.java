package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.Collectors.toLinkedHashMap;
import static org.folio.roles.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.roles.utils.CollectionUtils.toSet;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.integration.kafka.model.ResourceEventType;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilityService {

  public static final String VISIBLE_PERMISSION_PREFIXES = buildPrefixesParam(List.of("ui-", "module", "plugin"));

  private final CapabilityRepository capabilityRepository;
  private final FolioExecutionContext folioExecutionContext;
  private final CapabilityEntityMapper capabilityEntityMapper;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Lazy private final CapabilitySetService capabilitySetService;

  /**
   * Creates folio resources from incoming resource event.
   *
   * @param eventType - {@link ResourceEventType} value
   * @param newCapabilities - list of {@link Capability} records
   * @param oldCapabilities - list of old {@link Capability} records
   */
  @Transactional
  public void update(ResourceEventType eventType, List<Capability> newCapabilities, List<Capability> oldCapabilities) {
    log.info("Capabilities received: new = {}, old = {}", newCapabilities.size(), oldCapabilities.size());

    var capabilityNames = toSet(newCapabilities, Capability::getName);
    var foundCapabilitiesByName = findExistingCapabilitiesByNames(capabilityNames);
    var groupedCapabilities = newCapabilities.stream()
      .collect(groupingBy(capability -> foundCapabilitiesByName.containsKey(capability.getName())));

    handleNewCapabilities(groupedCapabilities.get(Boolean.FALSE));
    handleUpdatedCapabilities(eventType, groupedCapabilities.get(Boolean.TRUE), foundCapabilitiesByName);
    handleDeprecatedCapabilities(oldCapabilities, capabilityNames);
  }

  /**
   * Retrieves capability by ID.
   *
   * @param capabilityId - capability identifier
   * @return found {@link Capability} object
   */
  @Transactional(readOnly = true)
  public Capability get(UUID capabilityId) {
    var capabilityEntity = capabilityRepository.getReferenceById(capabilityId);
    return capabilityEntityMapper.convert(capabilityEntity);
  }

  /**
   * Retrieves capabilities by CQL query and pagination parameters.
   *
   * @param query - CQL query as {@link String}
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record
   * @return {@link PageResult} object with found {@link Capability} records
   */
  @Transactional(readOnly = true)
  public PageResult<Capability> find(String query, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var capabilityEntities = capabilityRepository.findByQuery(query, offsetRequest);
    var capabilitiesPage = capabilityEntities.map(capabilityEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Retrieves capabilities by user id.
   *
   * @param userId - user identifier as {@link UUID} object
   * @param expand - defines if capability sets myst be expanded
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public PageResult<Capability> findByUserId(UUID userId, boolean expand, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var capabilityEntitiesPage = expand
      ? capabilityRepository.findAllByUserId(userId, offsetRequest)
      : capabilityRepository.findByUserId(userId, offsetRequest);

    var capabilitiesPage = capabilityEntitiesPage.map(capabilityEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Retrieves capabilities by role id.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @param expand - defines if capability sets myst be expanded
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public PageResult<Capability> findByRoleId(UUID roleId, boolean expand, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var capabilityEntitiesPage = expand
      ? capabilityRepository.findAllByRoleId(roleId, offsetRequest)
      : capabilityRepository.findByRoleId(roleId, offsetRequest);

    var capabilitiesPage = capabilityEntitiesPage.map(capabilityEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Retrieves capabilities by capability names.
   *
   * @param capabilityNames - list of {@link String} capability names
   * @return found {@link Capability} object
   */
  @Transactional(readOnly = true)
  public List<Capability> findByNames(Collection<String> capabilityNames) {
    var capabilityEntities = capabilityRepository.findAllByNames(capabilityNames);
    return capabilityEntityMapper.convert(capabilityEntities);
  }

  /**
   * Retrieves capability by capability name.
   *
   * @param capabilityName - capability name
   * @return found {@link Capability} object
   */
  @Transactional(readOnly = true)
  public Optional<Capability> findByName(String capabilityName) {
    var capabilityEntity = capabilityRepository.findByName(capabilityName);
    return capabilityEntity.map(capabilityEntityMapper::convert);
  }

  /**
   * Retrieves capability by permission name.
   *
   * @param permissionName - permission name
   * @return found {@link Capability} object
   */
  @Transactional(readOnly = true)
  public Optional<Capability> findByPermissionName(String permissionName) {
    var capabilityEntity = capabilityRepository.findByPermission(permissionName);
    return capabilityEntity.map(capabilityEntityMapper::convert);
  }

  /**
   * Retrieves capabilities by permission names no technical capabilities included.
   *
   * @param permissionNames - list of {@link String} permission names
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public List<Capability> findByPermissionNamesNoTechnical(Collection<String> permissionNames) {
    var capabilityEntities = capabilityRepository.findAllByPermissionNames(permissionNames);
    return toStream(capabilityEntityMapper.convert(capabilityEntities))
      .filter(not(CapabilityUtils::isTechnicalCapability))
      .toList();
  }

  /**
   * Retrieves capabilities by permission names.
   *
   * @param permissionNames - list of {@link String} permission names
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public List<Capability> findByPermissionNames(Collection<String> permissionNames) {
    if (isEmpty(permissionNames)) {
      return emptyList();
    }
    var capabilityEntities = capabilityRepository.findAllByPermissionNames(permissionNames);
    return capabilityEntityMapper.convert(capabilityEntities);
  }

  /**
   * Retrieves capabilities by capability set id and pagination parameters.
   *
   * @param capabilitySetId - capability set identifier
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} object with found {@link Capability} records
   */
  @Transactional(readOnly = true)
  public PageResult<Capability> findByCapabilitySetId(UUID capabilitySetId, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    capabilitySetService.get(capabilitySetId);
    var capabilityEntities = capabilityRepository.findByCapabilitySetId(capabilitySetId, offsetRequest);
    var capabilitiesPage = capabilityEntities.map(capabilityEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Retrieves capabilities by capability set ids.
   *
   * @param capabilitySetIds - list with capability set identifiers
   * @return {@link List} with found {@link Capability} records
   */
  @Transactional(readOnly = true)
  public List<Capability> findByCapabilitySetIds(Collection<UUID> capabilitySetIds) {
    var capabilityEntities = capabilityRepository.findByCapabilitySetIds(capabilitySetIds);
    return capabilityEntityMapper.convert(capabilityEntities);
  }

  /**
   * Checks existing capability ids.
   *
   * @param capabilityIds - collection with capability {@link UUID} identifiers
   * @throws EntityNotFoundException if some of the capability ids are not found in the database
   */
  @Transactional(readOnly = true)
  public void checkIds(Collection<UUID> capabilityIds) {
    if (CollectionUtils.isEmpty(capabilityIds)) {
      return;
    }

    var capabilityIdsToCheck = new LinkedHashSet<>(capabilityIds);
    var foundCapabilityIds = capabilityRepository.findCapabilityIdsByIdIn(capabilityIdsToCheck);
    if (foundCapabilityIds.size() != capabilityIdsToCheck.size()) {
      var notFoundCapabilityIds = CollectionUtils.subtract(capabilityIdsToCheck, foundCapabilityIds);
      throw new EntityNotFoundException("Capabilities not found by ids: " + notFoundCapabilityIds);
    }
  }

  /**
   * Checks existing capability ids.
   *
   * @param capabilityIds - collection with capability {@link UUID} identifiers
   * @return found capability ids
   */
  @Transactional(readOnly = true)
  public List<Capability> findByIds(Collection<UUID> capabilityIds) {
    return capabilityRepository.findAllById(capabilityIds).stream()
      .map(capabilityEntityMapper::convert)
      .toList();
  }

  /**
   * Retrieves user permissions by userId and onlyVisible flag. If desiredPermissions list is not empty, then all
   * permissions matching the desired ones will be returned. Wildcard (*) is supported for desired permissions. Example:
   * user has permission ["users.item.get", "users.item.post", "users.collection.put"] and requested permissions are
   * ["users.item.*"] then resolved permissions will be ["users.item.get", "users.item.post"].
   *
   * @param userId - user identifier as {@link UUID} value
   * @param onlyVisible - defines if UI or all permissions must be returned
   * @param desiredPermissions - list of desired permissions to find
   * @return a {@link List} with folio permission names
   */
  @Transactional(readOnly = true)
  public List<String> getUserPermissions(UUID userId, boolean onlyVisible, List<String> desiredPermissions) {
    if (onlyVisible) {
      return capabilityRepository.findPermissionsByPrefixes(userId, VISIBLE_PERMISSION_PREFIXES);
    }

    if (isNotEmpty(desiredPermissions)) {
      var prefixes =
        toStream(desiredPermissions).filter(s -> s.contains("*")).map(CapabilityService::trimWildcard).toList();
      var permNames = toStream(desiredPermissions).filter(s -> !s.contains("*")).toList();
      return capabilityRepository.findPermissionsByPrefixesAndPermissionNames(userId, buildPrefixesParam(permNames),
        buildPrefixesParam(prefixes));
    }

    return capabilityRepository.findAllFolioPermissions(userId);
  }

  @Transactional
  public void deleteById(UUID capabilityId) {
    capabilityRepository.deleteById(capabilityId);
  }

  /**
   * Raises version for existing capability sets by module id + application id.
   *
   * @param moduleId - module identifier
   * @param newApplicationId - new application identifier
   * @param oldApplicationId - old application identifier
   */
  public void updateApplicationVersion(String moduleId, String newApplicationId, String oldApplicationId) {
    capabilityRepository.updateApplicationVersion(moduleId, newApplicationId, oldApplicationId);
  }

  /**
   * Raises version for existing capability sets by module name + application name.
   *
   * @param applicationName - application name
   * @param moduleName - module name
   * @param newApplicationId - new application identifier
   * @param newModuleId - new module identifier
   */
  public void updateAppAndModuleVersionByAppAndModuleName(String applicationName, String moduleName,
    String newApplicationId, String newModuleId) {
    capabilityRepository.updateAppAndModuleVersionByAppAndModuleName(applicationName, moduleName, newApplicationId,
      newModuleId);
  }

  private static String trimWildcard(String param) {
    return param.endsWith("*") ? param.substring(0, param.length() - 1) : param;
  }

  private static String buildPrefixesParam(List<String> prefixes) {
    return toStream(prefixes).collect(joining(", ", "{", "}"));
  }

  private Map<String, CapabilityEntity> findExistingCapabilitiesByNames(Set<String> capabilityNames) {
    if (isEmpty(capabilityNames)) {
      return emptyMap();
    }

    return capabilityRepository.findAllByNames(capabilityNames).stream()
      .collect(toLinkedHashMap(CapabilityEntity::getName));
  }

  private void handleNewCapabilities(List<Capability> capabilities) {
    if (isEmpty(capabilities)) {
      return;
    }

    var capabilityEntities = mapItems(capabilities, capabilityEntityMapper::convert);
    var savedCapabilityEntities = capabilityRepository.saveAll(capabilityEntities);
    for (var savedCapabilityEntity : savedCapabilityEntities) {
      var capability = capabilityEntityMapper.convert(savedCapabilityEntity);
      var event = CapabilityEvent.created(capability).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }

  private void handleUpdatedCapabilities(ResourceEventType type,
    List<Capability> capabilities, Map<String, CapabilityEntity> capabilitiesByName) {
    if (isEmpty(capabilities)) {
      return;
    }

    var capabilityEntities = new ArrayList<CapabilityEntity>();
    var oldCapabilitiesById = new LinkedHashMap<UUID, Capability>();

    for (var updatedCapability : capabilities) {
      var capabilityEntity = capabilitiesByName.get(updatedCapability.getName());
      var capabilityId = capabilityEntity.getId();
      updatedCapability.setId(capabilityId);
      capabilityEntities.add(capabilityEntityMapper.convert(updatedCapability));
      oldCapabilitiesById.put(capabilityId, capabilityEntityMapper.convert(capabilityEntity));
    }

    var updatedCapabilityEntities = capabilityRepository.saveAll(capabilityEntities);
    if (type == CREATE) {
      log.warn("Duplicated capabilities has been updated: {}", () ->
        mapItems(oldCapabilitiesById.values(), Capability::getName));
    }

    for (var updatedCapabilityEntity : updatedCapabilityEntities) {
      var newCapability = capabilityEntityMapper.convert(updatedCapabilityEntity);
      var oldCapability = oldCapabilitiesById.get(newCapability.getId());
      var event = CapabilityEvent.updated(newCapability, oldCapability).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }

  private void handleDeprecatedCapabilities(List<Capability> oldCapabilities, Set<String> capabilityNames) {
    var deprecatedCapabilityNames = toStream(oldCapabilities)
      .map(Capability::getName)
      .filter(not(capabilityNames::contains))
      .collect(Collectors.toSet());

    if (isEmpty(deprecatedCapabilityNames)) {
      return;
    }

    var deprecatedCapabilityEntities = capabilityRepository.findAllByNames(deprecatedCapabilityNames);
    if (isEmpty(deprecatedCapabilityEntities)) {
      return;
    }

    for (var deprecatedCapabilityEntity : deprecatedCapabilityEntities) {
      var deprecatedCapability = capabilityEntityMapper.convert(deprecatedCapabilityEntity);
      var event = CapabilityEvent.deleted(deprecatedCapability).withContext(folioExecutionContext);
      applicationEventPublisher.publishEvent(event);
    }
  }
}
