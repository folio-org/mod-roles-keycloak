package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.service.event.DomainEvent;
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

  private final String[] visiblePermissionPrefixes = new String[] {"ui-", "module", "plugin"};
  private final CapabilityRepository capabilityRepository;
  private final CapabilityEntityMapper capabilityEntityMapper;
  @Lazy private final CapabilitySetService capabilitySetService;
  private final ApplicationEventPublisher eventPublisher;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Creates folio resources from incoming resource event.
   *
   * @param applicationId - application identifier as {@link String}
   * @param capabilities - list of {@link Capability} records
   */
  @Transactional
  public void createSafe(String applicationId, List<Capability> capabilities) {
    if (isEmpty(capabilities)) {
      return;
    }

    log.info("{} capabilities received", capabilities.size());
    var capabilityNames = new HashSet<String>();
    for (var capability : capabilities) {
      var capabilityName = getCapabilityName(capability.getResource(), capability.getAction());
      capability.setName(capabilityName);
      capability.setApplicationId(applicationId);
      capabilityNames.add(capabilityName);
    }

    var foundCapabilityNames = capabilityRepository.findCapabilityNames(capabilityNames);
    var savedEntities = saveCapabilities(capabilities, foundCapabilityNames);

    if (isNotEmpty(savedEntities)) {
      var saved = capabilityEntityMapper.convert(savedEntities);
      eventPublisher.publishEvent(DomainEvent.created(saved).withContext(folioExecutionContext));
    }
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
      .collect(toList());
  }

  /**
   * Retrieves user permissions by userId and onlyVisible flag.
   *
   * @param userId - user identifier as {@link UUID} value
   * @param onlyVisible - defines if UI or all permissions must be returned
   * @return a {@link List} with folio permission names
   */
  @Transactional(readOnly = true)
  public List<String> getUserPermissions(UUID userId, boolean onlyVisible) {
    if (!onlyVisible) {
      return capabilityRepository.findAllFolioPermissions(userId);
    }

    String permissionPrefixesParam = Arrays.stream(visiblePermissionPrefixes)
      .collect(joining(", ", "{", "}"));

    return capabilityRepository.findVisibleFolioPermissions(userId, permissionPrefixesParam);
  }

  private List<CapabilityEntity> saveCapabilities(List<Capability> capabilities, Set<String> foundNames) {
    var capabilityEntities = new ArrayList<CapabilityEntity>();
    for (var capability : capabilities) {
      var capabilityName = capability.getName();
      if (foundNames.contains(capabilityName)) {
        log.warn("Capability by name already exists: name = {}", capabilityName);
        continue;
      }
      capabilityEntities.add(capabilityEntityMapper.convert(capability));
    }

    return isNotEmpty(capabilityEntities) ? capabilityRepository.saveAll(capabilityEntities) : emptyList();
  }
}
