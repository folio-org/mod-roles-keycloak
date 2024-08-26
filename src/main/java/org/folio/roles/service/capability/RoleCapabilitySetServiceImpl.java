package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.intersection;
import static org.apache.commons.collections4.ListUtils.subtract;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.entity.RoleCapabilitySetEntity.DEFAULT_ROLE_CAPABILITY_SET_SORT;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityEndpoints;
import static org.folio.roles.utils.CollectionUtils.difference;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.RoleCapabilitySet;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.mapper.entity.RoleCapabilitySetEntityMapper;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.utils.UpdateOperationHelper;
import org.folio.spring.data.OffsetRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Primary
@Service
@Transactional
@RequiredArgsConstructor
public class RoleCapabilitySetServiceImpl implements RoleCapabilitySetService {

  private final RoleService roleService;
  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final RolePermissionService rolePermissionService;
  private final CapabilityEndpointService capabilityEndpointService;
  private final RoleCapabilitySetRepository roleCapabilitySetRepository;
  private final RoleCapabilitySetEntityMapper roleCapabilitySetEntityMapper;

  @Override
  @Transactional
  public PageResult<RoleCapabilitySet> create(UUID roleId, List<UUID> capabilitySetIds, boolean safeCreate) {
    return createByIds(roleId, capabilitySetIds, safeCreate);
  }

  @Override
  @Transactional
  public PageResult<RoleCapabilitySet> create(RoleCapabilitySetsRequest request, boolean safeCreate) {
    verifyRequest(request);
    if (isEmpty(request.getCapabilitySetIds())) {
      return createByNames(request.getRoleId(), request.getCapabilitySetNames(), safeCreate);
    }
    return createByIds(request.getRoleId(), request.getCapabilitySetIds(), safeCreate);
  }

  /**
   * Retrieves role-capabilitySets items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} with found {@link RoleCapabilitySet} relations
   */
  @Override
  @Transactional(readOnly = true)
  public PageResult<RoleCapabilitySet> find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_ROLE_CAPABILITY_SET_SORT);
    var entities = roleCapabilitySetRepository.findByQuery(query, offsetRequest);
    var roleCapabilities = entities.map(roleCapabilitySetEntityMapper::convert);
    return PageResult.of(roleCapabilities.getTotalElements(), roleCapabilities.getContent());
  }

  /**
   * Updates a list of assigned to a role capabilitySets.
   *
   * @param roleId - role identifier
   * @param capabilitySetIds - list with new capabilitySets, that should be assigned to a role
   */
  @Override
  @Transactional
  public void update(UUID roleId, List<UUID> capabilitySetIds) {
    roleService.getById(roleId);
    var assignedRoleCapabilitySetEntities = roleCapabilitySetRepository.findAllByRoleId(roleId);

    var assignedSetIds = getCapabilitySetIds(assignedRoleCapabilitySetEntities);
    UpdateOperationHelper.create(assignedSetIds, capabilitySetIds, "role-capability set")
      .consumeAndCacheNewEntities(newIds -> getCapabilitySetIds(assignCapabilities(roleId, newIds, assignedSetIds)))
      .consumeDeprecatedEntities((deprecatedIds, createdIds) -> removeCapabilities(roleId, deprecatedIds, createdIds));
  }

  /**
   * Removes role assigned capability set using role identifier and capability set id.
   *
   * @param roleId  - role identifier as {@link UUID}
   * @param capabilitySetId  - capability set identifier as {@link UUID}
   */
  @Override
  @Transactional
  public void delete(UUID roleId, UUID capabilitySetId) {
    var assignedRoleCapabilitySetEntities = roleCapabilitySetRepository.findAllByRoleId(roleId);
    var assignedCapabilitySetIds = getCapabilitySetIds(assignedRoleCapabilitySetEntities);
    if (!assignedCapabilitySetIds.contains(capabilitySetId)) {
      return;
    }

    assignedCapabilitySetIds.remove(capabilitySetId);
    roleCapabilitySetRepository.findById(RoleCapabilitySetKey.of(roleId, capabilitySetId))
      .ifPresent(entity -> removeCapabilities(roleId, List.of(entity.getCapabilitySetId()), assignedCapabilitySetIds));
  }

  /**
   * Removes role assigned capability sets using role identifier and capability set ids.
   *
   * @param roleId - role identifier as {@link UUID}
   * @param capabilitySetIds - list with capabilitySet ids, that should be removed from a role
   */
  @Override
  @Transactional
  public void delete(UUID roleId, List<UUID> capabilitySetIds) {
    if (isEmpty(capabilitySetIds)) {
      return;
    }

    var assignedRoleCapabilitySetEntities = roleCapabilitySetRepository.findAllByRoleId(roleId);
    var assignedCapabilitySetIds = getCapabilitySetIds(assignedRoleCapabilitySetEntities);

    var deprecatedIds = intersection(assignedCapabilitySetIds, capabilitySetIds);
    if (isEmpty(deprecatedIds)) {
      return;
    }

    removeCapabilities(roleId, deprecatedIds, subtract(assignedCapabilitySetIds, deprecatedIds));
  }

  /**
   * Removes role assigned capabilities using role identifier.
   *
   * @param roleId - role identifier as {@link UUID}
   * @throws jakarta.persistence.EntityNotFoundException if role is not found by id or there is no assigned values
   */
  @Override
  @Transactional
  public void deleteAll(UUID roleId) {
    roleService.getById(roleId);
    var roleCapabilitySetEntities = roleCapabilitySetRepository.findAllByRoleId(roleId);
    if (isEmpty(roleCapabilitySetEntities)) {
      throw new EntityNotFoundException("Relations between role and capability sets are not found for role: " + roleId);
    }

    var capabilitySetIds = getCapabilitySetIds(roleCapabilitySetEntities);
    removeCapabilities(roleId, capabilitySetIds, emptyList());
  }

  private PageResult<RoleCapabilitySet> createByIds(UUID roleId, List<UUID> capabilitySetIds, boolean safeCreate) {
    if (isEmpty(capabilitySetIds)) {
      throw new IllegalArgumentException("List with capability set identifiers is empty");
    }

    roleService.getById(roleId);
    var existingEntities = roleCapabilitySetRepository.findRoleCapabilitySets(roleId, capabilitySetIds);
    var existingCapabilitySetIds = getCapabilitySetIds(existingEntities);
    if (!safeCreate && isNotEmpty(existingCapabilitySetIds)) {
      throw new EntityExistsException(String.format(
        "Relation already exists for role='%s' and capabilitySets=%s", roleId, existingCapabilitySetIds));
    }

    var newSetIds = difference(capabilitySetIds, existingCapabilitySetIds);
    return isEmpty(newSetIds) ? PageResult.empty() : assignCapabilities(roleId, newSetIds, emptyList());
  }

  private PageResult<RoleCapabilitySet> createByNames(UUID roleId, List<String> capabilitySetNames,
                                                      boolean safeCreate) {
    var capabilitySetIds = capabilitySetService.findByNames(capabilitySetNames).stream()
      .map(CapabilitySet::getId)
      .toList();
    return createByIds(roleId, capabilitySetIds, safeCreate);
  }

  private PageResult<RoleCapabilitySet> assignCapabilities(
    UUID roleId, List<UUID> newSetIds, Collection<UUID> assignedSetIds) {
    log.debug("Assigning capabilities to role: roleId = {}, ids = {}", roleId, newSetIds);
    capabilitySetService.checkIds(newSetIds);

    var entities = mapItems(newSetIds, capabilitySetId -> new RoleCapabilitySetEntity(roleId, capabilitySetId));
    var changedEndpoints = getChangedEndpoints(roleId, newSetIds, assignedSetIds);
    rolePermissionService.createPermissions(roleId, changedEndpoints);

    var resultEntities = roleCapabilitySetRepository.saveAll(entities);
    var createdRoleCapabilities = mapItems(resultEntities, roleCapabilitySetEntityMapper::convert);
    log.info("Capabilities assigned to role: roleId = {}, ids = {}", roleId, newSetIds);

    return PageResult.of(createdRoleCapabilities.size(), createdRoleCapabilities);
  }

  private void removeCapabilities(UUID roleId, List<UUID> deprecatedSetIds, Collection<UUID> assignedSetIds) {
    log.debug("Revoking capabilities from role: roleId = {}, ids = {}", roleId, deprecatedSetIds);
    var changedEndpoints = getChangedEndpoints(roleId, deprecatedSetIds, assignedSetIds);
    rolePermissionService.deletePermissions(roleId, changedEndpoints);
    roleCapabilitySetRepository.deleteRoleCapabilitySets(roleId, deprecatedSetIds);
    log.info("Capability sets are revoked to role: roleId = {}, ids = {}", roleId, deprecatedSetIds);
  }

  private List<Endpoint> getChangedEndpoints(UUID roleId, List<UUID> deprecatedIds, Collection<UUID> assignedIds) {
    var directlyAssignedCapabilities = capabilityService.findByRoleId(roleId, false, MAX_VALUE, 0);
    var excludedEndpoints = getCapabilityEndpoints(directlyAssignedCapabilities.getRecords());
    return capabilityEndpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, excludedEndpoints);
  }

  private static List<UUID> getCapabilitySetIds(PageResult<RoleCapabilitySet> roleCapabilitySets) {
    return mapItems(roleCapabilitySets.getRecords(), RoleCapabilitySet::getCapabilitySetId);
  }

  private static List<UUID> getCapabilitySetIds(List<RoleCapabilitySetEntity> existingEntities) {
    return mapItems(existingEntities, RoleCapabilitySetEntity::getCapabilitySetId);
  }

  private void verifyRequest(RoleCapabilitySetsRequest request) {
    if (isEmpty(request.getCapabilitySetIds()) && isEmpty(request.getCapabilitySetNames())) {
      throw new IllegalArgumentException("'capabilitySetIds' or 'capabilitySetNames' must not be null");
    }
  }
}
