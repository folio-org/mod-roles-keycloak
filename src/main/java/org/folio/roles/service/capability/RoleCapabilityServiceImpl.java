package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.subtract;
import static org.apache.commons.collections4.ListUtils.intersection;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.entity.RoleCapabilityEntity.DEFAULT_ROLE_CAPABILITY_SORT;
import static org.folio.roles.domain.model.event.TenantPermissionsChangedEvent.tenantPermissionsChanged;
import static org.folio.roles.utils.CollectionUtils.difference;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.mapper.entity.RoleCapabilityEntityMapper;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.roles.utils.CollectionUtils;
import org.folio.roles.utils.KeycloakTransactionHelper;
import org.folio.roles.utils.UpdateOperationHelper;
import org.folio.spring.data.OffsetRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Primary
@Service
@Transactional
@RequiredArgsConstructor
public class RoleCapabilityServiceImpl implements RoleCapabilityService {

  private final RoleService roleService;
  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final RolePermissionService rolePermissionService;
  private final RoleCapabilityRepository roleCapabilityRepository;
  private final CapabilityEndpointService capabilityEndpointService;
  private final RoleCapabilityEntityMapper roleCapabilityEntityMapper;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Creates a record(s) associating one or more capabilities with the role.
   *
   * @param roleId        - role identifier as {@link UUID} object
   * @param capabilityIds - capability identifiers as {@link List} of {@link UUID}
   *                      objects
   * @param safeCreate    - defines if new capabilities must be added or error
   *                      thrown if any already exists
   * @return {@link RoleCapability} object with created role-capability relations
   */
  @Override
  @Transactional
  public PageResult<RoleCapability> create(UUID roleId, List<UUID> capabilityIds, boolean safeCreate) {
    var result = createRoleCapabilities(roleId, capabilityIds, safeCreate);
    eventPublisher.publishEvent(tenantPermissionsChanged());
    return result;
  }

  /**
   * Creates record(s) associating one or more capabilities with a role.
   *
   * @param request    - request containing roleId, capabilityIds or
   *                   capabilityNames
   * @param safeCreate - defines if new capabilities must be added or error thrown
   *                   if any already exists
   * @return {@link RoleCapability} object with created role-capability relations
   */
  @Override
  @Transactional
  public PageResult<RoleCapability> create(RoleCapabilitiesRequest request, boolean safeCreate) {
    var resolvedCapabilitiesIds = resolveCapabilitiesByNames(request.getCapabilityNames());
    var allCapabilityIds = CollectionUtils.union(resolvedCapabilitiesIds, request.getCapabilityIds());
    var result = createRoleCapabilities(request.getRoleId(), allCapabilityIds, safeCreate);
    eventPublisher.publishEvent(tenantPermissionsChanged());
    return result;
  }

  /**
   * Retrieves role-capability items by CQL query.
   *
   * @param query  - CQL query as {@link String} object
   * @param limit  - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} object with found {@link RoleCapability} relation
   *         descriptors.
   */
  @Override
  @Transactional(readOnly = true)
  public PageResult<RoleCapability> find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_ROLE_CAPABILITY_SORT);
    var entities = roleCapabilityRepository.findByQuery(query, offsetRequest);
    var roleCapabilities = entities.map(roleCapabilityEntityMapper::convert);
    return PageResult.fromPage(roleCapabilities);
  }

  /**
   * Updates role-capability relations.
   *
   * @param roleId        - role identifier as {@link UUID} object
   * @param capabilityIds - list of capabilities that must be assigned to a role
   */
  @Override
  @Transactional
  public void update(UUID roleId, List<UUID> capabilityIds) {
    updateRoleCapabilities(roleId, capabilityIds);
    eventPublisher.publishEvent(tenantPermissionsChanged());
  }

  /**
   * Updates role-capability relations.
   *
   * @param roleId  - role identifier as {@link UUID} object
   * @param request - CapabilitiesUpdateRequest that contains either capability
   *                IDs or names, to be assigned to a
   *                role
   */
  @Override
  @Transactional
  public void update(UUID roleId, CapabilitiesUpdateRequest request) {
    var resolvedCapabilitiesIds = resolveCapabilitiesByNames(request.getCapabilityNames());
    var allCapabilityIds = CollectionUtils.union(resolvedCapabilitiesIds, request.getCapabilityIds());
    updateRoleCapabilities(roleId, allCapabilityIds);
    eventPublisher.publishEvent(tenantPermissionsChanged());
  }

  /**
   * Removes role-capability relations by role and capability identifiers.
   *
   * @param roleId       - role identifier as {@link UUID}
   * @param capabilityId - capability identifier as {@link UUID}
   */
  @Override
  @Transactional
  public void delete(UUID roleId, UUID capabilityId) {
    var assignedRoleCapabilityEntities = roleCapabilityRepository.findAllByRoleId(roleId);
    var assignedCapabilityIds = getCapabilityIds(assignedRoleCapabilityEntities);
    if (!assignedCapabilityIds.contains(capabilityId)) {
      return;
    }

    assignedCapabilityIds.remove(capabilityId);
    roleCapabilityRepository.findById(RoleCapabilityKey.of(roleId, capabilityId))
        .ifPresent(entity -> removeCapabilities(roleId, List.of(entity.getCapabilityId()), assignedCapabilityIds));
    eventPublisher.publishEvent(tenantPermissionsChanged());
  }

  /**
   * Removes role-capability relations by role identifier and capability ids.
   *
   * @param roleId        - role identifier as {@link UUID}
   * @param capabilityIds - list with capabilities, that should be removed from a
   *                      role
   */
  @Override
  @Transactional
  public void delete(UUID roleId, List<UUID> capabilityIds) {
    if (isEmpty(capabilityIds)) {
      return;
    }

    var assignedRoleCapabilityEntities = roleCapabilityRepository.findAllByRoleId(roleId);
    var assignedCapabilityIds = getCapabilityIds(assignedRoleCapabilityEntities);

    var deprecatedIds = intersection(assignedCapabilityIds, capabilityIds);
    if (isEmpty(deprecatedIds)) {
      return;
    }

    removeCapabilities(roleId, deprecatedIds, subtract(assignedCapabilityIds, deprecatedIds));
    eventPublisher.publishEvent(tenantPermissionsChanged());
  }

  /**
   * Removes all role-capability relations by role identifier.
   *
   * @param roleId - role identifier as {@link UUID}
   * @throws EntityNotFoundException if role is not found by id or there is no
   *                                 assigned values
   */
  @Override
  @Transactional
  public void deleteAll(UUID roleId) {
    roleService.getById(roleId);
    var roleCapabilityEntities = roleCapabilityRepository.findAllByRoleId(roleId);
    if (isEmpty(roleCapabilityEntities)) {
      throw new EntityNotFoundException("Relations between role and capabilities are not found for role: " + roleId);
    }

    removeCapabilities(roleId, getCapabilityIds(roleCapabilityEntities), emptyList());
    eventPublisher.publishEvent(tenantPermissionsChanged());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UUID> getCapabilitySetCapabilityIds(UUID roleId) {
    return getAssignedCapabilityIds(roleId);
  }

  private PageResult<RoleCapability> createRoleCapabilities(UUID roleId, List<UUID> capabilityIds, boolean safeCreate) {
    if (isEmpty(capabilityIds)) {
      throw new IllegalArgumentException("Capability id list is empty");
    }

    roleService.getById(roleId);
    var existingEntities = roleCapabilityRepository.findRoleCapabilities(roleId, capabilityIds);
    var existingCapabilityIds = getCapabilityIds(existingEntities);
    if (!safeCreate && isNotEmpty(existingCapabilityIds)) {
      throw new EntityExistsException(String.format(
          "Relation already exists for role='%s' and capabilities=%s", roleId, existingCapabilityIds));
    }

    var newCapabilityIds = difference(capabilityIds, existingCapabilityIds);
    return isEmpty(newCapabilityIds) ? PageResult.empty() : assignCapabilities(roleId, newCapabilityIds, emptyList());
  }

  private void updateRoleCapabilities(UUID roleId, List<UUID> capabilityIds) {
    roleService.getById(roleId);
    var assignedRoleCapabilityEntities = roleCapabilityRepository.findAllByRoleId(roleId);
    var assignedCapabilityIds = getCapabilityIds(assignedRoleCapabilityEntities);
    UpdateOperationHelper.create(assignedCapabilityIds, capabilityIds, "role-capability")
        .consumeAndCacheNewEntities(
            newIds -> getCapabilityIds(assignCapabilities(roleId, newIds, assignedCapabilityIds)))
        .consumeDeprecatedEntities(
            (deprecatedIds, createdIds) -> removeCapabilities(roleId, deprecatedIds, createdIds));
  }

  private List<UUID> resolveCapabilitiesByNames(List<String> capabilityNames) {
    if (isEmpty(capabilityNames)) {
      return emptyList();
    }

    var foundCapabilitiesByNames = capabilityService.findByNames(capabilityNames);
    var foundCapabilitiesNames = mapItems(foundCapabilitiesByNames, Capability::getName);
    var notFoundCapabilities = difference(capabilityNames, foundCapabilitiesNames);

    if (isEmpty(foundCapabilitiesNames)) {
      throw new RequestValidationException("Capabilities by name are not found",
          "capabilityNames", notFoundCapabilities);
    }

    if (isNotEmpty(notFoundCapabilities)) {
      log.warn("resolveCapabilitiesByNames:: Found non existing capabilityNames: {}", notFoundCapabilities);
    }

    return mapItems(foundCapabilitiesByNames, Capability::getId);
  }

  private PageResult<RoleCapability> assignCapabilities(UUID roleId, List<UUID> newIds, Collection<UUID> assignedIds) {
    log.debug("Assigning capabilities to role: roleId = {}, capabilityIds = {}", roleId, newIds);
    capabilityService.checkIds(newIds);

    var entities = mapItems(newIds, id -> new RoleCapabilityEntity(roleId, id));
    var assignedCapabilityIds = CollectionUtils.union(assignedIds, getAssignedCapabilityIds(roleId));
    var endpoints = capabilityEndpointService.getByCapabilityIds(newIds, assignedCapabilityIds);

    var resultEntities = KeycloakTransactionHelper.executeWithCompensation(
        () -> rolePermissionService.createPermissions(roleId, endpoints),
        () -> roleCapabilityRepository.saveAll(entities),
        () -> rolePermissionService.deletePermissions(roleId, endpoints));

    var createdRoleCapabilities = mapItems(resultEntities, roleCapabilityEntityMapper::convert);
    log.info("Capabilities are assigned to role: roleId = {}, capabilityIds = {}", roleId, newIds);

    return PageResult.of(createdRoleCapabilities.size(), createdRoleCapabilities);
  }

  private void removeCapabilities(UUID roleId, List<UUID> deprecatedIds, Collection<UUID> assignedIds) {
    log.debug("Revoking capabilities from role: roleId = {}, capabilityIds = {}", roleId, deprecatedIds);
    var assignedCapabilityIds = CollectionUtils.union(assignedIds, getAssignedCapabilityIds(roleId));
    var endpoints = capabilityEndpointService.getByCapabilityIds(deprecatedIds, assignedCapabilityIds);

    KeycloakTransactionHelper.executeWithCompensation(
        () -> rolePermissionService.deletePermissions(roleId, endpoints),
        () -> roleCapabilityRepository.deleteRoleCapabilities(roleId, deprecatedIds),
        () -> rolePermissionService.createPermissions(roleId, endpoints));

    log.info("Capabilities are revoked to role: roleId = {}, capabilityIds = {}", roleId, deprecatedIds);
  }

  private static List<UUID> getCapabilityIds(List<RoleCapabilityEntity> roleCapabilityEntities) {
    return mapItems(roleCapabilityEntities, RoleCapabilityEntity::getCapabilityId);
  }

  private static List<UUID> getCapabilityIds(PageResult<RoleCapability> roleCapabilities) {
    return mapItems(roleCapabilities.getRecords(), RoleCapability::getCapabilityId);
  }

  private List<UUID> getAssignedCapabilityIds(UUID roleId) {
    var capabilitySets = capabilitySetService.findByRoleId(roleId, MAX_VALUE, 0);
    return CapabilityUtils.getCapabilitySetIds(capabilitySets);
  }
}
