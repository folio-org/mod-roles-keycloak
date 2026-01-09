package org.folio.roles.service.role;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.common.utils.CollectionUtils.mapItems;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.dto.UserRoles;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.roles.integration.keyclock.KeycloakRolesUserService;
import org.folio.roles.utils.UpdateOperationHelper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleService {

  private final RoleService roleService;
  private final UserRoleEntityService userRoleEntityService;
  private final KeycloakRolesUserService keycloakRolesUserService;

  /**
   * Creates a new user-role relation based on the provided {@link UserRolesRequest} object.
   *
   * @param userRolesRequest - {@link List} with {@link UserRole} records to create
   * @return created {@link UserRoles} relations object
   */
  @CacheEvict(value = "user-permissions", key = "@folioExecutionContext.tenantId + ':' + #userRolesRequest.userId")
  @Transactional
  public UserRoles create(UserRolesRequest userRolesRequest) {
    var userId = userRolesRequest.getUserId();
    var roleIds = userRolesRequest.getRoleIds();
    var foundRoles = roleService.findByIds(roleIds);
    var createdUserRoles = userRoleEntityService.create(userId, roleIds);
    keycloakRolesUserService.assignRolesToUser(userId, foundRoles);
    return buildUserRoles(createdUserRoles);
  }

  /**
   * Creates a relation between user and role by their identifiers.
   *
   * @param userRole - user-role relation
   */
  @Transactional
  public void createSafe(UserRole userRole) {
    var roleById = roleService.getById(userRole.getRoleId());
    var foundUserRole = userRoleEntityService.find(userRole);
    if (foundUserRole.isPresent()) {
      return;
    }

    keycloakRolesUserService.assignRolesToUser(userRole.getUserId(), singletonList(roleById));
    userRoleEntityService.createSafe(userRole);
  }

  /**
   * Updates an existing {@link UserRole} relations based on the provided {@link UserRolesRequest}.
   *
   * @param request {@link UserRolesRequest} containing the updated information about user-role relations
   */
  @CacheEvict(value = "user-permissions", key = "@folioExecutionContext.tenantId + ':' + #request.userId")
  @Transactional
  public void update(UserRolesRequest request) {
    var userId = request.getUserId();
    var existingRoleIds = mapItems(userRoleEntityService.findByUserId(userId), UserRole::getRoleId);

    UpdateOperationHelper.create(existingRoleIds, request.getRoleIds(), "user-role")
      .consumeNewEntities(newValues -> createNewRoles(newValues, userId))
      .consumeDeprecatedEntities(deprecatedValues -> deleteDeprecatedRoles(deprecatedValues, userId));
  }

  /**
   * Finds a {@link UserRoles} relation items with the user id.
   *
   * @param userId - user identifier as {@link UUID} object
   * @return found {@link UserRoles} relation items
   */
  public UserRoles findById(UUID userId) {
    return buildUserRoles(userRoleEntityService.findByUserId(userId));
  }

  /**
   * Finds {@link UserRoles} based on a provided CQL query, limit and offset values.
   *
   * @param query - the query used to find records
   * @param offset - the offset of records to return
   * @param limit - the maximum number of records to return
   * @return a {@link UserRole} object containing the {@link UserRole} relation items
   */
  public UserRoles findByQuery(String query, Integer offset, Integer limit) {
    return userRoleEntityService.findByQuery(query, offset, limit);
  }

  /**
   * Deletes a `RolesUser` with the specified id.
   *
   * @param userId - user identifier as {@link UUID} value.
   */
  @CacheEvict(value = "user-permissions", key = "@folioExecutionContext.tenantId + ':' + #userId")
  @Transactional
  public void deleteById(UUID userId) {
    var roleIds = mapItems(userRoleEntityService.findByUserId(userId), UserRole::getRoleId);
    userRoleEntityService.deleteByUserId(userId);
    var roles = roleService.findByIds(roleIds);
    keycloakRolesUserService.unlinkRolesFromUser(userId, roles);
  }

  private static UserRoles buildUserRoles(List<UserRole> userRoles) {
    var userRolesList = emptyIfNull(userRoles);
    return new UserRoles().userRoles(userRolesList).totalRecords(userRolesList.size());
  }

  private void createNewRoles(List<UUID> newValues, UUID userId) {
    var roles = roleService.findByIds(newValues);
    userRoleEntityService.create(userId, newValues);
    keycloakRolesUserService.assignRolesToUser(userId, roles);
  }

  private void deleteDeprecatedRoles(List<UUID> deprecatedValues, UUID userId) {
    var roles = roleService.findByIds(deprecatedValues);
    userRoleEntityService.delete(userId, deprecatedValues);
    keycloakRolesUserService.unlinkRolesFromUser(userId, roles);
  }
}
