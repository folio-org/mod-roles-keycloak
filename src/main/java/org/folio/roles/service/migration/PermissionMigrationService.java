package org.folio.roles.service.migration;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.exception.MigrationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class PermissionMigrationService {

  private final MigrationRoleCreator migrationRoleCreator;
  private final UserPermissionsLoader userPermissionsLoader;
  private final RolePermissionAssignor rolePermissionAssignor;
  private final ManagePermissionsResolver managePermissionsResolver;

  /**
   * Runs permission migration.
   *
   * <ul>
   *   <li>Loads users from keycloak and extracts folio user identifiers</li>
   *   <li>Loads user permissions from mod-permissions</li>
   *   <li>Created roles based on hash (sha1) of ordered list of loaded permissions</li>
   *   <li>Assigns capabilities/capability sets to a role based on folio permission name</li>
   * </ul>
   */
  @Transactional
  public void migratePermissions(UUID jobId) {
    log.info("Starting permission migration: jobId = {}", jobId);

    var userPermissions = userPermissionsLoader.loadUserPermissions();
    var createdRoles = migrationRoleCreator.createRoles(userPermissions);
    userPermissions = validateAndGetUserPermissionsWithRoles(userPermissions, createdRoles);

    migrationRoleCreator.assignUsers(userPermissions);
    managePermissionsResolver.addManageCapabilities(userPermissions);
    rolePermissionAssignor.assignPermissions(userPermissions);

    log.info("Migration of permissions is finished: jobId = {}", jobId);
  }

  private static List<UserPermissions> validateAndGetUserPermissionsWithRoles(
    List<UserPermissions> userPermissions, List<Role> createdRoles) {
    var createdRolesByName = toHashMap(createdRoles, Role::getName);
    var errorUserPermissions = new ArrayList<UserPermissions>();
    var resultUserPermissions = new ArrayList<UserPermissions>();
    for (var userPermission : userPermissions) {
      var roleName = userPermission.getRoleName();
      var createdRoleByName = createdRolesByName.get(roleName);
      if (createdRoleByName == null) {
        errorUserPermissions.add(userPermission);
        continue;
      }

      resultUserPermissions.add(userPermission.role(createdRoleByName));
    }

    if (isNotEmpty(errorUserPermissions)) {
      throw new MigrationException("Roles are not created for user permissions: " + errorUserPermissions);
    }

    return resultUserPermissions;
  }

  private static <K, V> Map<K, V> toHashMap(Collection<V> collection, Function<V, K> keyMapper) {
    return emptyIfNull(collection).stream().collect(toMap(keyMapper, identity(), (o1, o2) -> o2));
  }
}
