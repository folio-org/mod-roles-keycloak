package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.entity.UserCapabilitySetEntity.DEFAULT_USER_CAPABILITY_SET_SORT;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityEndpoints;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.UserCapabilitySet;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.key.UserCapabilitySetKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.mapper.entity.UserCapabilitySetEntityMapper;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.utils.UpdateOperationHelper;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class UserCapabilitySetService {

  private final CapabilityService capabilityService;
  private final KeycloakUserService keycloakUserService;
  private final CapabilitySetService capabilitySetService;
  private final UserPermissionService userPermissionService;
  private final CapabilityEndpointService capabilityEndpointService;
  private final UserCapabilitySetRepository userCapabilitySetRepository;
  private final UserCapabilitySetEntityMapper userCapabilitySetEntityMapper;

  /**
   * Creates a record(s) associating one or more capabilitySets with a user.
   *
   * @param userId - user identifier as {@link UUID} object
   * @param capabilitySetIds - capabilitySet identifiers as {@link List} of {@link UUID} objects
   * @return {@link PageResult} with created {@link UserCapabilitySet} relations
   */
  @Transactional
  public PageResult<UserCapabilitySet> create(UUID userId, List<UUID> capabilitySetIds) {
    if (isEmpty(capabilitySetIds)) {
      throw new IllegalArgumentException("List with capability set identifiers is empty");
    }

    keycloakUserService.getKeycloakUserByUserId(userId);
    var existingEntities = userCapabilitySetRepository.findUserCapabilitySets(userId, capabilitySetIds);
    var existingCapabilitySetIds = getCapabilitySetIds(existingEntities);
    if (isNotEmpty(existingCapabilitySetIds)) {
      throw new EntityExistsException(String.format(
        "Relation already exists for user='%s' and capabilitySets=%s", userId, existingCapabilitySetIds));
    }

    return assignCapabilities(userId, capabilitySetIds, emptyList());
  }

  /**
   * Retrieves user-capabilitySets items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} with found {@link UserCapabilitySet} relations
   */
  @Transactional(readOnly = true)
  public PageResult<UserCapabilitySet> find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_USER_CAPABILITY_SET_SORT);
    var entities = userCapabilitySetRepository.findByQuery(query, offsetRequest);
    var userCapabilities = entities.map(userCapabilitySetEntityMapper::convert);
    return PageResult.of(userCapabilities.getTotalElements(), userCapabilities.getContent());
  }

  /**
   * Updates a list of assigned to a user capabilitySets.
   *
   * @param userId - user identifier
   * @param capabilityIds - list with new capabilitySets, that should be assigned to a user
   */
  @Transactional
  public void update(UUID userId, List<UUID> capabilityIds) {
    keycloakUserService.getKeycloakUserByUserId(userId);
    var assignedUserCapabilitySetEntities = userCapabilitySetRepository.findAllByUserId(userId);

    var assignedSetIds = getCapabilitySetIds(assignedUserCapabilitySetEntities);
    UpdateOperationHelper.create(assignedSetIds, capabilityIds, "user-capability set")
      .consumeAndCacheNewEntities(newIds -> getCapabilitySetIds(assignCapabilities(userId, newIds, assignedSetIds)))
      .consumeDeprecatedEntities((deprecatedIds, createdIds) -> removeCapabilities(userId, deprecatedIds, createdIds));
  }

  /**
   * Removes user assigned capability set using user identifier and capability set id.
   *
   * @param userId - role identifier as {@link UUID}
   * @param capabilitySetId - capability set identifier as {@link UUID}
   */
  @Transactional
  public void delete(UUID userId, UUID capabilitySetId) {
    var assignedUserCapabilitySetEntities = userCapabilitySetRepository.findAllByUserId(userId);
    var assignedCapabilitySetIds = getCapabilitySetIds(assignedUserCapabilitySetEntities);
    if (!assignedCapabilitySetIds.contains(capabilitySetId)) {
      return;
    }

    assignedCapabilitySetIds.remove(capabilitySetId);
    userCapabilitySetRepository.findById(UserCapabilitySetKey.of(userId, capabilitySetId))
      .ifPresent(entity -> removeCapabilities(userId, List.of(entity.getCapabilitySetId()), assignedCapabilitySetIds));
  }

  /**
   * Removes user assigned capabilities using user identifier.
   *
   * @param userId - user identifier as {@link UUID}
   * @throws jakarta.persistence.EntityNotFoundException if user is not found by id or there is no assigned values
   */
  @Transactional
  public void deleteAll(UUID userId) {
    keycloakUserService.getKeycloakUserByUserId(userId);
    var userCapabilitySetEntities = userCapabilitySetRepository.findAllByUserId(userId);
    if (isEmpty(userCapabilitySetEntities)) {
      throw new EntityNotFoundException("Relations between user and capability sets are not found for user: " + userId);
    }

    var capabilitySetIds = getCapabilitySetIds(userCapabilitySetEntities);
    removeCapabilities(userId, capabilitySetIds, emptyList());
  }

  private PageResult<UserCapabilitySet> assignCapabilities(
    UUID userId, List<UUID> newSetIds, Collection<UUID> assignedSetIds) {
    log.debug("Assigning capabilities to user: userId = {}, ids = {}", userId, newSetIds);
    capabilitySetService.checkIds(newSetIds);

    var entities = mapItems(newSetIds, capabilitySetId -> new UserCapabilitySetEntity(userId, capabilitySetId));
    var changedEndpoints = getChangedEndpoints(userId, newSetIds, assignedSetIds);
    userPermissionService.createPermissions(userId, changedEndpoints);

    var resultEntities = userCapabilitySetRepository.saveAll(entities);
    var createdUserCapabilities = mapItems(resultEntities, userCapabilitySetEntityMapper::convert);
    log.info("Capabilities assigned to user: userId = {}, ids = {}", userId, newSetIds);

    return PageResult.of(createdUserCapabilities.size(), createdUserCapabilities);
  }

  private void removeCapabilities(UUID userId, List<UUID> deprecatedSetIds, Collection<UUID> assignedSetIds) {
    log.debug("Revoking capabilities from user: userId = {}, ids = {}", userId, deprecatedSetIds);
    var changedEndpoints = getChangedEndpoints(userId, deprecatedSetIds, assignedSetIds);
    userPermissionService.deletePermissions(userId, changedEndpoints);
    userCapabilitySetRepository.deleteUserCapabilitySets(userId, deprecatedSetIds);
    log.info("Capability sets are revoked to user: userId = {}, ids = {}", userId, deprecatedSetIds);
  }

  private List<Endpoint> getChangedEndpoints(UUID userId, List<UUID> deprecatedIds, Collection<UUID> assignedIds) {
    var directlyAssignedCapabilities = capabilityService.findByUserId(userId, false, false, MAX_VALUE, 0);
    var excludedEndpoints = getCapabilityEndpoints(directlyAssignedCapabilities.getRecords());
    return capabilityEndpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, excludedEndpoints);
  }

  private static List<UUID> getCapabilitySetIds(PageResult<UserCapabilitySet> userCapabilitySets) {
    return mapItems(userCapabilitySets.getRecords(), UserCapabilitySet::getCapabilitySetId);
  }

  private static List<UUID> getCapabilitySetIds(List<UserCapabilitySetEntity> existingEntities) {
    return mapItems(existingEntities, UserCapabilitySetEntity::getCapabilitySetId);
  }
}
