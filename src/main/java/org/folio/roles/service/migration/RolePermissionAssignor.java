package org.folio.roles.service.migration;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.utils.CollectionUtils.difference;
import static org.folio.roles.utils.CollectionUtils.unionUniqueValues;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class RolePermissionAssignor {

  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final RoleCapabilityService roleCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;

  /**
   * Assigns permissions to role in user permissions list.
   *
   * @param userPermissions - user permissions object
   */
  public void assignPermissions(List<UserPermissions> userPermissions) {
    var visitedRoleIds = new HashSet<UUID>();
    for (var userPermission : userPermissions) {
      var roleId = userPermission.getRole().getId();
      if (visitedRoleIds.contains(roleId)) {
        continue;
      }

      assignCapabilitiesAndSets(roleId, userPermission.getPermissions());
      visitedRoleIds.add(roleId);
    }
  }

  private void assignCapabilitiesAndSets(UUID roleId, List<String> permissions) {
    log.debug("Assigning capabilities/capability sets for role: roleId = {}", roleId);

    var capabilities = capabilityService.findByPermissionNamesNoTechnical(permissions);
    var notFoundPermissions = difference(permissions, mapItems(capabilities, Capability::getPermission));
    if (isNotEmpty(capabilities)) {
      roleCapabilityService.create(roleId, mapItems(capabilities, Capability::getId), true);
    }

    var sets = capabilitySetService.findByPermissionNames(permissions);
    var notFoundPermissionsInSets = difference(notFoundPermissions, mapItems(sets, CapabilitySet::getPermission));
    if (isNotEmpty(sets)) {
      roleCapabilitySetService.create(roleId, mapItems(sets, CapabilitySet::getId), true);
    }

    log.debug("Capabilities/capability sets assigned: roleId = {}, permissions = {}",
      () -> roleId, () -> difference(permissions, notFoundPermissionsInSets));

    if (isNotEmpty(notFoundPermissionsInSets)) {
      log.warn("Permissions are not assigned: roleId = {}, permissions = {}",
        () -> roleId, () -> unionUniqueValues(notFoundPermissions, notFoundPermissionsInSets));
    }
  }
}
