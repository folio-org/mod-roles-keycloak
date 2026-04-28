package org.folio.roles.service.role;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.dto.PolicyType.ROLE;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.integration.keyclock.KeycloakPolicyService;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.integration.keyclock.KeycloakRolesUserService;
import org.folio.roles.service.capability.CapabilityEndpointService;
import org.folio.roles.service.policy.PolicyEntityService;
import org.folio.roles.service.policy.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Log4j2
@Service
@RequiredArgsConstructor
public class RoleService {

  private final KeycloakRoleService keycloakService;
  private final KeycloakRolesUserService keycloakRolesUserService;
  private final KeycloakAuthorizationService keycloakAuthService;
  private final KeycloakPolicyService keycloakPolicyService;
  private final RoleEntityService entityService;
  private final UserRoleEntityService userRoleEntityService;
  private final PolicyEntityService policyEntityService;
  private final PolicyService policyService;
  private final CapabilityEndpointService capabilityEndpointService;

  /**
   * Find one role by ID.
   *
   * @param id - role identifier
   * @return existing role
   */
  @Transactional(readOnly = true)
  public Role getById(UUID id) {
    return entityService.getById(id);
  }

  /**
   * Find by ids.
   *
   * @param ids - role identifiers
   * @return existing role
   */
  @Transactional(readOnly = true)
  public List<Role> findByIds(List<UUID> ids) {
    var roles = entityService.findByIds(ids);
    if (roles.size() != ids.size()) {
      var notFoundIds = ListUtils.subtract(ids, mapItems(roles, Role::getId));
      throw new EntityNotFoundException("Roles are not found for ids: " + notFoundIds);
    }

    return roles;
  }

  /**
   * Search roles by query or/and offset or/and limit. Searching without parameters returns all roles.
   *
   * @param query - query for searching roles
   * @param offset - offset for searching roles
   * @param limit - limit for searching roles
   * @return {@link Roles} which contains array of {@link Role} and number of elements in the roles array
   */
  @Transactional(readOnly = true)
  public Roles search(String query, Integer offset, Integer limit) {
    var roles = entityService.findByQuery(query, offset, limit);
    return buildRoles(roles);
  }

  /**
   * Create a role.
   *
   * @param role - role object for creating
   * @return created {@link Role} object
   * @throws RequestValidationException if some of the provided fields are invalid
   */
  @Transactional
  public Role create(Role role) {
    checkIfRoleHasDefaultType(role);
    var createdRole = keycloakService.create(role);
    try {
      createdRole.setType(role.getType());
      Role savedRole = entityService.create(createdRole);
      log.debug("Role has been created: id = {}, name = {}", savedRole.getId(), savedRole.getName());
      return savedRole;
    } catch (Exception exception) {
      keycloakService.deleteById(createdRole.getId());
      throw new ServiceException("Failed to create role", exception);
    }
  }

  /**
   * Create one or more roles.
   *
   * @param roles - roles for creating
   * @return {@link Roles} which contains array of {@link Role} and number of elements in the roles array
   */
  @Transactional
  public Roles create(List<Role> roles) {
    var createdRoles = roles.stream()
      .map(this::create)
      .toList();
    return buildRoles(createdRoles);
  }

  /**
   * Update one role.
   *
   * @param role - role for updating
   * @return updated role {@link Role}
   */
  @Transactional
  public Role update(Role role) {
    Assert.notNull(role.getId(), "Role should has ID");
    var actualRole = entityService.getById(role.getId());
    checkIfRoleHasDefaultType(role);
    checkIfRoleHasDefaultType(actualRole);
    keycloakService.update(role);
    try {
      return entityService.update(role);
    } catch (Exception e) {
      log.debug("Rollback role state in Keycloak: id = {}, name = {}", actualRole.getId(), actualRole.getName());
      keycloakService.update(actualRole);
      throw e;
    }
  }

  /**
   * Delete role by ID.
   *
   * @param id - role identifier
   */
  @Transactional
  public void deleteById(UUID id) {
    var actualRole = entityService.getById(id);
    checkIfRoleHasDefaultType(actualRole);
    var cleanupData = cleanupRoleData(actualRole);
    var roleDeletedFromDb = false;
    try {
      entityService.deleteById(id);
      roleDeletedFromDb = true;
      keycloakService.deleteById(id);
    } catch (Exception e) {
      log.debug("Rollback deleted policy in db: id = {}, name = {}", actualRole.getId(), actualRole.getName());
      rollbackRoleCleanup(actualRole, cleanupData);
      if (roleDeletedFromDb) {
        entityService.create(actualRole);
      }
      throw e;
    }
  }

  private RoleCleanupData cleanupRoleData(Role role) {
    var roleId = role.getId();
    var cleanupData = new RoleCleanupData();
    var policy = findRolePolicy(roleId);
    if (policy != null) {
      cleanupData.policy = policy;
      cleanupData.endpoints = capabilityEndpointService.getRoleAssignedEndpoints(roleId, emptyList(), emptyList());
    }

    try {
      cleanupUsersFromRole(role, cleanupData);
      userRoleEntityService.deleteByRoleId(roleId);
      deleteRolePolicyData(roleId, cleanupData);
      return cleanupData;
    } catch (Exception e) {
      rollbackRoleCleanup(role, cleanupData);
      throw e;
    }
  }

  private void cleanupUsersFromRole(Role role, RoleCleanupData cleanupData) {
    var roleId = role.getId();
    var assignedUserRoles = userRoleEntityService.findByRoleId(roleId);
    for (var userId : mapItems(assignedUserRoles, UserRole::getUserId)) {
      keycloakRolesUserService.unlinkRolesFromUser(userId, List.of(role));
      cleanupData.unlinkedUserIds.add(userId);
    }
  }

  private Policy findRolePolicy(UUID roleId) {
    var policyName = getPolicyName(roleId);
    var policyOptional = policyEntityService.findByName(policyName);
    if (policyOptional.isEmpty()) {
      log.debug("Role policy is not found. Cleanup of permissions and policy is skipped: roleId = {}", roleId);
      return null;
    }

    var policy = policyOptional.get();
    if (policy.getType() != ROLE) {
      log.warn("Role policy has unexpected type. Cleanup of permissions and policy is skipped: roleId = {}, type = {}",
        roleId, policy.getType());
      return null;
    }

    return policy;
  }

  private void deleteRolePolicyData(UUID roleId, RoleCleanupData cleanupData) {
    var policy = cleanupData.policy;
    if (policy == null) {
      return;
    }

    cleanupData.permissionsDeletionAttempted = true;
    keycloakAuthService.deletePermissions(policy, cleanupData.endpoints, getPermissionNameGenerator(roleId));
    policyService.deleteById(policy.getId());
    cleanupData.policyDeleted = true;
  }

  private void rollbackRoleCleanup(Role role, RoleCleanupData cleanupData) {
    rollbackRolePolicyCleanup(role.getId(), cleanupData);
    for (var userId : cleanupData.unlinkedUserIds) {
      try {
        keycloakRolesUserService.assignRolesToUser(userId, List.of(role));
      } catch (Exception e) {
        log.warn("Failed to restore role assignment in Keycloak: roleId = {}, userId = {}", role.getId(), userId, e);
      }
    }
  }

  private void rollbackRolePolicyCleanup(UUID roleId, RoleCleanupData cleanupData) {
    var policy = cleanupData.policy;
    if (policy == null) {
      return;
    }

    try {
      if (cleanupData.policyDeleted) {
        keycloakPolicyService.create(policy);
      }
      if (cleanupData.permissionsDeletionAttempted) {
        keycloakAuthService.createPermissions(policy, cleanupData.endpoints, getPermissionNameGenerator(roleId));
      }
    } catch (Exception e) {
      log.warn("Failed to restore role policy data in Keycloak: roleId = {}", roleId, e);
    }
  }

  private static Roles buildRoles(List<Role> roles) {
    return new Roles().roles(roles).totalRecords((long) roles.size());
  }

  private static Roles buildRoles(PageResult<Role> roles) {
    return new Roles().roles(roles.getRecords()).totalRecords(roles.getTotalRecords());
  }

  private static Function<Endpoint, String> getPermissionNameGenerator(UUID roleId) {
    return endpoint -> format("%s access for role '%s' to '%s'", endpoint.getMethod(), roleId, endpoint.getPath());
  }

  private static String getPolicyName(UUID roleId) {
    return "Policy for role: " + roleId;
  }

  private static void checkIfRoleHasDefaultType(Role role) {
    if (hasDefaultRoleType(role)) {
      log.debug("Default role cannot be created, updated or deleted via roles API: id = {}, name = {}", role.getId(),
        role.getName());
      throw new IllegalArgumentException("Default role cannot be created, updated or deleted via roles API.");
    }
  }

  private static boolean hasDefaultRoleType(Role role) {
    return RoleType.DEFAULT == role.getType();
  }

  private static final class RoleCleanupData {

    private final List<UUID> unlinkedUserIds = new ArrayList<>();
    private Policy policy;
    private List<Endpoint> endpoints = emptyList();
    private boolean permissionsDeletionAttempted;
    private boolean policyDeleted;
  }
}
