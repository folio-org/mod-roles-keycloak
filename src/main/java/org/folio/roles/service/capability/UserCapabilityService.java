package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.UserCapabilities;
import org.folio.roles.domain.dto.UserCapability;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.key.UserCapabilityKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.integration.userskc.ModUsersKeycloakClient;
import org.folio.roles.mapper.entity.UserCapabilityEntityMapper;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.roles.utils.CollectionUtils;
import org.folio.roles.utils.UpdateOperationHelper;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class UserCapabilityService {

  private final CapabilityService capabilityService;
  private final KeycloakUserService keycloakUserService;
  private final CapabilitySetService capabilitySetService;
  private final UserPermissionService userPermissionService;
  private final UserCapabilityRepository userCapabilityRepository;
  private final CapabilityEndpointService capabilityEndpointService;
  private final UserCapabilityEntityMapper userCapabilityEntityMapper;
  private final ModUsersKeycloakClient modUsersKeycloakClient;

  /**
   * Creates a record(s) associating one or more capabilities with the user.
   *
   * @param userId - user identifier as {@link UUID} object
   * @param capabilityIds - capability identifiers as {@link List} of {@link UUID} objects
   * @return {@link UserCapabilities} object with created user-capability relations
   */
  @Transactional
  public PageResult<UserCapability> create(UUID userId, List<UUID> capabilityIds) {
    if (isEmpty(capabilityIds)) {
      throw new IllegalArgumentException("Capability id list is empty");
    }

    modUsersKeycloakClient.ensureKeycloakUserExists(userId.toString());
    var existingEntities = userCapabilityRepository.findUserCapabilities(userId, capabilityIds);
    var existingCapabilitySetIds = getCapabilityIds(existingEntities);
    if (isNotEmpty(existingCapabilitySetIds)) {
      throw new EntityExistsException(String.format(
        "Relation already exists for user='%s' and capabilities=%s", userId, existingCapabilitySetIds));
    }

    return assignCapabilities(userId, capabilityIds, emptyList());
  }

  /**
   * Retrieves user-capability items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} object with found {@link UserCapability} relation descriptors.
   */
  @Transactional(readOnly = true)
  public PageResult<UserCapability> find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, UserCapabilityEntity.DEFAULT_USER_CAPABILITY_SORT);
    var entities = userCapabilityRepository.findByQuery(query, offsetRequest);
    var userCapabilities = entities.map(userCapabilityEntityMapper::convert);
    return PageResult.fromPage(userCapabilities);
  }

  /**
   * Updates user-capability relations.
   *
   * @param userId - user identifier as {@link UUID} object
   * @param capabilityIds - list of capabilities that must be assigned to a user
   */
  @Transactional
  public void update(UUID userId, List<UUID> capabilityIds) {
    keycloakUserService.getKeycloakUserByUserId(userId);
    var assignedUserCapabilityEntities = userCapabilityRepository.findAllByUserId(userId);
    var assignedCapabilityIds = getCapabilityIds(assignedUserCapabilityEntities);
    UpdateOperationHelper.create(assignedCapabilityIds, capabilityIds, "user-capability")
      .consumeAndCacheNewEntities(newIds -> getCapabilityIds(assignCapabilities(userId, newIds, assignedCapabilityIds)))
      .consumeDeprecatedEntities((deprecatedIds, createdIds) -> removeCapabilities(userId, deprecatedIds, createdIds));
  }

  /**
   * Removes user-capability relations by user and capability identifiers.
   *
   * @param userId - user identifier as {@link UUID}
   * @param capabilityId - capability identifier as {@link UUID}
   */
  @Transactional
  public void delete(UUID userId, UUID capabilityId) {
    var assignedUserCapabilityEntities = userCapabilityRepository.findAllByUserId(userId);
    var assignedCapabilityIds = getCapabilityIds(assignedUserCapabilityEntities);
    if (!assignedCapabilityIds.contains(capabilityId)) {
      return;
    }

    assignedCapabilityIds.remove(capabilityId);
    userCapabilityRepository.findById(UserCapabilityKey.of(userId, capabilityId))
      .ifPresent(entity -> removeCapabilities(userId, List.of(entity.getCapabilityId()), assignedCapabilityIds));
  }

  /**
   * Removes all user-capability relations by user identifier.
   *
   * @param userId - user identifier as {@link UUID}
   * @throws EntityNotFoundException if user is not found by id or there is no assigned values
   */
  @Transactional
  public void deleteAll(UUID userId) {
    keycloakUserService.getKeycloakUserByUserId(userId);
    var userCapabilityEntities = userCapabilityRepository.findAllByUserId(userId);
    if (isEmpty(userCapabilityEntities)) {
      throw new EntityNotFoundException("Relations between user and capabilities are not found for user: " + userId);
    }

    removeCapabilities(userId, getCapabilityIds(userCapabilityEntities), emptyList());
  }

  private PageResult<UserCapability> assignCapabilities(UUID userId, List<UUID> newIds, Collection<UUID> assignedIds) {
    log.debug("Assigning capabilities to user: userId = {}, ids = {}", userId, newIds);
    capabilityService.checkIds(newIds);

    var entities = mapItems(newIds, id -> new UserCapabilityEntity(userId, id));
    var assignedCapabilityIds = CollectionUtils.union(assignedIds, getCapabilitySetCapabilityIds(userId));
    var endpoints = capabilityEndpointService.getByCapabilityIds(newIds, assignedCapabilityIds);
    userPermissionService.createPermissions(userId, endpoints);

    var resultEntities = userCapabilityRepository.saveAll(entities);
    var createdUserCapabilities = mapItems(resultEntities, userCapabilityEntityMapper::convert);
    log.info("Capabilities are assigned to user: userId = {}, ids = {}", userId, newIds);

    return PageResult.of(createdUserCapabilities.size(), createdUserCapabilities);
  }

  private void removeCapabilities(UUID userId, List<UUID> deprecatedIds, Collection<UUID> assignedIds) {
    log.debug("Revoking capabilities from user: userId = {}, ids = {}", userId, deprecatedIds);
    var assignedCapabilityIds = CollectionUtils.union(assignedIds, getCapabilitySetCapabilityIds(userId));
    var endpoints = capabilityEndpointService.getByCapabilityIds(deprecatedIds, assignedCapabilityIds);
    userPermissionService.deletePermissions(userId, endpoints);
    userCapabilityRepository.deleteUserCapabilities(userId, deprecatedIds);
    log.info("Capabilities are revoked to user: userId = {}, ids = {}", userId, deprecatedIds);
  }

  private static List<UUID> getCapabilityIds(List<UserCapabilityEntity> userCapabilityEntities) {
    return mapItems(userCapabilityEntities, UserCapabilityEntity::getCapabilityId);
  }

  private static List<UUID> getCapabilityIds(PageResult<UserCapability> userCapabilities) {
    return mapItems(userCapabilities.getRecords(), UserCapability::getCapabilityId);
  }

  public List<UUID> getCapabilitySetCapabilityIds(UUID userId) {
    var capabilitySets = capabilitySetService.findByUserId(userId, MAX_VALUE, 0);
    return CapabilityUtils.getCapabilityIds(capabilitySets);
  }
}
