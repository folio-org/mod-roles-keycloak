package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.SetUtils.difference;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.domain.entity.CapabilitySetEntity.DEFAULT_CAPABILITY_SET_SORT;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class CapabilitySetService {

  private final CapabilityService capabilityService;
  private final CapabilitySetRepository repository;
  private final CapabilitySetEntityMapper capabilitySetEntityMapper;

  /**
   * Creates a capability.
   *
   * @param capabilitySet - capability object to create
   * @return created {@link Capability} object
   */
  @Transactional
  public CapabilitySet create(CapabilitySet capabilitySet) {
    requireNonNull(capabilitySet, "Capability set must not be null");
    if (repository.existsByName(capabilitySet.getName()) && !repository.existsById(capabilitySet.getId())) {
      throw new RequestValidationException("Capability set name is already taken", "name", capabilitySet.getName());
    }

    capabilityService.checkIds(capabilitySet.getCapabilities());

    var capabilityEntity = capabilitySetEntityMapper.convert(capabilitySet);
    var savedEntity = repository.save(capabilityEntity);

    return capabilitySetEntityMapper.convert(savedEntity);
  }

  /**
   * Creates capabilities from batch request.
   *
   * @param capabilitySets - capability sets batch request
   * @return {@link PageResult} with created {@link Capability} objects
   */
  @Transactional
  public List<CapabilitySet> createAll(Collection<CapabilitySet> capabilitySets) {
    if (isEmpty(capabilitySets)) {
      return emptyList();
    }

    var createdCapabilitySets = new ArrayList<CapabilitySet>();
    for (var capabilitySet : capabilitySets) {
      try {
        createdCapabilitySets.add(create(capabilitySet));
      } catch (Exception exception) {
        log.warn("Failed to create capability set: resource = {}, action = {}",
          capabilitySet.getResource(), capabilitySet.getAction(), exception);
      }
    }

    return createdCapabilitySets;
  }

  /**
   * Retrieves capabilitySets items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} with found {@link CapabilitySets} records
   */
  @Transactional(readOnly = true)
  public PageResult<CapabilitySet> find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_CAPABILITY_SET_SORT);
    var capabilitySetPage = repository.findByQuery(query, offsetRequest).map(capabilitySetEntityMapper::convert);
    return PageResult.fromPage(capabilitySetPage);
  }

  @Transactional(readOnly = true)
  public List<CapabilitySet> find(List<UUID> capabilityIds) {
    if (isEmpty(capabilityIds)) {
      return emptyList();
    }

    var caps = repository.findAllById(capabilityIds);
    return capabilitySetEntityMapper.convert(caps);
  }

  /**
   * Removes capability sets by id.
   *
   * @param id - capability set identifier as {@link UUID}
   */
  @Transactional
  public void delete(UUID id) {
    var capabilitySetEntity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Capability set is not found: id = " + id));

    repository.delete(capabilitySetEntity);
  }

  @Transactional(readOnly = true)
  public List<CapabilitySet> getCapabilities(Collection<UUID> capabilityIds) {
    if (isEmpty(capabilityIds)) {
      return emptyList();
    }

    var foundEntities = repository.findAllById(capabilityIds);
    if (foundEntities.size() != capabilityIds.size()) {
      var foundCapabilityIds = foundEntities.stream()
        .map(CapabilitySetEntity::getId)
        .collect(toCollection(LinkedHashSet::new));

      var notFoundCapabilityIds = difference(new LinkedHashSet<>(capabilityIds), foundCapabilityIds);
      throw new EntityNotFoundException("Capabilities are not found by ids: " + notFoundCapabilityIds);
    }

    return capabilitySetEntityMapper.convert(foundEntities);
  }

  @Transactional(readOnly = true)
  public CapabilitySet get(UUID id) {
    var capabilitySetEntity = repository.getReferenceById(id);
    return capabilitySetEntityMapper.convert(capabilitySetEntity);
  }

  @Transactional
  public void update(UUID id, CapabilitySet capabilitySet) {
    var capabilitySetId = capabilitySet.getId();
    if (capabilitySetId == null) {
      throw new RequestValidationException("must not be null", "id", null);
    }

    if (!Objects.equals(id, capabilitySetId)) {
      throw new RequestValidationException("Id from path and in entity does not match", "id", capabilitySetId);
    }

    var foundEntity = repository.getReferenceById(id);
    var newCapabilityName = getCapabilityName(capabilitySet.getResource(), capabilitySet.getAction());

    if (!Objects.equals(foundEntity.getName(), newCapabilityName)) {
      if (repository.existsByName(newCapabilityName)) {
        throw new RequestValidationException("Capability set name is already taken", "name", newCapabilityName);
      }

      capabilitySet.setName(newCapabilityName);
    }

    if (isBlank(capabilitySet.getName())) {
      capabilitySet.setName(foundEntity.getName());
    }

    capabilityService.checkIds(capabilitySet.getCapabilities());
    var entity = capabilitySetEntityMapper.convert(capabilitySet);
    repository.saveAndFlush(entity);
  }

  /**
   * Retrieves capabilities by user id.
   *
   * @param userId - user identifier as {@link UUID} object
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public PageResult<CapabilitySet> findByUserId(UUID userId, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_CAPABILITY_SET_SORT);
    var capabilityEntitiesPage = repository.findByUserId(userId, offsetRequest);
    var capabilitiesPage = capabilityEntitiesPage.map(capabilitySetEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Retrieves capabilities by role id.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @return list with {@link Capability} objects
   */
  @Transactional(readOnly = true)
  public PageResult<CapabilitySet> findByRoleId(UUID roleId, int limit, int offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_CAPABILITY_SET_SORT);
    var capabilityEntitiesPage = repository.findByRoleId(roleId, offsetRequest);
    var capabilitiesPage = capabilityEntitiesPage.map(capabilitySetEntityMapper::convert);
    return PageResult.fromPage(capabilitiesPage);
  }

  /**
   * Finds capability set by name.
   *
   * @param capabilitySetName - capability set name as {@link String}
   * @return {@link Optional} of {@link CapabilitySet} object, {@link Optional#empty()} otherwise
   */
  @Transactional(readOnly = true)
  public Optional<CapabilitySet> findByName(String capabilitySetName) {
    return repository.findByName(capabilitySetName).map(capabilitySetEntityMapper::convert);
  }

  /**
   * Finds capability set by names.
   *
   * @param capabilitySetName - capability set name as {@link String}
   * @return {@link Optional} of {@link CapabilitySet} object, {@link Optional#empty()} otherwise
   */
  @Transactional(readOnly = true)
  public List<CapabilitySet> findByNames(Collection<String> capabilitySetName) {
    return mapItems(repository.findByNameIn(capabilitySetName), capabilitySetEntityMapper::convert);
  }

  @Transactional(readOnly = true)
  public List<CapabilitySet> findByPermissionNames(Collection<String> permissionNames) {
    return mapItems(repository.findByPermissionNames(permissionNames), capabilitySetEntityMapper::convert);
  }

  /**
   * Finds capability set by permission name.
   *
   * @param permissionName - permission name as {@link String}
   * @return {@link Optional} of {@link CapabilitySet} object, {@link Optional#empty()} otherwise
   */
  @Transactional(readOnly = true)
  public Optional<CapabilitySet> findByPermissionName(String permissionName) {
    return repository.findByPermission(permissionName).map(capabilitySetEntityMapper::convert);
  }

  @Transactional(readOnly = true)
  public List<CapabilitySet> findAllByCapabilityId(UUID capabilityId) {
    return toStream(repository.findAllByCapabilityId(capabilityId))
      .map(capabilitySetEntityMapper::convert)
      .toList();
  }

  /**
   * Checks existing capability ids.
   *
   * @param capabilitySetIds - collection with capability {@link UUID} identifiers
   * @throws EntityNotFoundException if some of the capability ids are not found in the database
   */
  @Transactional(readOnly = true)
  public void checkIds(Collection<UUID> capabilitySetIds) {
    if (isEmpty(capabilitySetIds)) {
      return;
    }

    checkByIds(capabilitySetIds);
  }

  @Transactional
  public void deleteById(UUID capabilitySetId) {
    repository.deleteById(capabilitySetId);
  }

  @Transactional
  public void deleteAllLinksToCapability(UUID capabilityId) {
    log.debug("Removing capability_set-capability links for capability: capabilityId = {}", capabilityId);
    repository.deleteCapabilityCapabilitySetLinks(capabilityId);
  }

  /**
   * Raises version for existing capability sets by module id + application id.
   *
   * @param moduleId - module identifier
   * @param newApplicationId - new application identifier
   * @param oldApplicationId - old application identifier
   */
  public void updateApplicationVersion(String moduleId, String newApplicationId, String oldApplicationId) {
    repository.updateApplicationVersion(moduleId, newApplicationId, oldApplicationId);
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
    repository.updateAppAndModuleVersionByAppAndModuleName(applicationName, moduleName, newApplicationId, newModuleId);
  }

  @Transactional(readOnly = true)
  public List<CapabilitySetEntity> findByCapabilityName(String capabilityName) {
    return repository.findByCapabilityName(capabilityName);
  }

  @Transactional
  public void addCapabilitiesById(UUID capabilitySetId, Collection<UUID> capabilityIds) {
    for (var capabilityId : capabilityIds) {
      repository.addCapabilityById(capabilitySetId, capabilityId);
    }
  }

  @Transactional
  public void updateAll(List<CapabilitySet> capabilitySets) {
    if (isEmpty(capabilitySets)) {
      return;
    }

    checkByIds(mapItems(capabilitySets, CapabilitySet::getId));

    var capabilitySetEntities = capabilitySetEntityMapper.mapToEntities(capabilitySets);
    repository.saveAll(capabilitySetEntities);
  }

  private void checkByIds(Collection<UUID> capabilitySetIds) {
    var capabilityIdsToCheck = new LinkedHashSet<>(capabilitySetIds);
    var foundCapabilityIds = repository.findCapabilitySetIdsByIdIn(capabilityIdsToCheck);
    if (foundCapabilityIds.size() != capabilityIdsToCheck.size()) {
      var notFoundCapabilityIds = CollectionUtils.subtract(capabilityIdsToCheck, foundCapabilityIds);
      throw new EntityNotFoundException("Capability sets not found by ids: " + notFoundCapabilityIds);
    }
  }
}
