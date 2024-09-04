package org.folio.roles.service.migration;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.service.role.UserRoleService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class MigrationRoleCreator {

  private final RoleService roleService;
  private final UserRoleService userRoleService;

  /**
   * Creates a list of role from user permissions.
   *
   * @param userPermissions - user permissions list
   * @return {@link List} with created {@link Role} objects
   */
  @Transactional
  public List<Role> createRoles(List<UserPermissions> userPermissions) {
    var roleNames = toStream(userPermissions)
      .map(UserPermissions::getRoleName)
      .distinct()
      .toList();

    log.info("Creating {} role(s) in keycloak...", roleNames.size());
    var roles = mapItems(roleNames, MigrationRoleCreator::createRole);
    var createdRoles = roleService.create(roles).getRoles();
    log.info("Roles created: totalRecords = {}", createdRoles.size());

    if (createdRoles.size() == roles.size()) {
      return createdRoles;
    }

    return toStream(roles)
      .map(role -> roleService.search("name==" + role.getName(), 0, roles.size()))
      .flatMap(rolesByName -> toStream(rolesByName.getRoles()))
      .toList();
  }

  /**
   * Assigns users to a role from the given list of {@link UserPermissions} objects.
   *
   * @param userPermissions - user permissions list
   */
  @Transactional
  public void assignUsers(List<UserPermissions> userPermissions) {
    var userByRole = groupUsersByRole(userPermissions);
    var counter = new AtomicInteger(0);
    var errorPairs = new ArrayList<UserRole>();
    for (var userByRoleEntry : userByRole.entrySet()) {
      var roleId = userByRoleEntry.getKey();
      for (var userId : userByRoleEntry.getValue()) {
        var userRole = new UserRole().userId(userId).roleId(roleId);
        try {
          userRoleService.createSafe(userRole);
          counter.getAndIncrement();
        } catch (Exception exception) {
          log.warn("Failed to assign user {} to role {}", userId, roleId, exception);
          errorPairs.add(userRole);
        }
      }
    }

    if (isNotEmpty(errorPairs)) {
      throw new MigrationException("Failed to assign users to roles: " + errorPairs);
    }

    log.info("User-role relations creation process finished: totalRecords = {}", counter.get());
  }

  private static Map<UUID, List<UUID>> groupUsersByRole(List<UserPermissions> userPermissions) {
    return toStream(userPermissions)
      .collect(groupingBy(up -> up.getRole().getId(), mapping(UserPermissions::getUserId, toList())));
  }

  private static Role createRole(String roleName) {
    return new Role()
      .name(roleName)
      .type(RoleType.DEFAULT)
      .description("System generated role during migration");
  }
}
