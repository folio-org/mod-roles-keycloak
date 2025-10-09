package org.folio.roles.service.loadablerole;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoadableRoleCapabilityAssignmentHelper {

  private final CapabilitySetService capabilitySetService;
  private final CapabilityService capabilityService;
  private final RoleCapabilityService roleCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;

  public Set<LoadablePermissionEntity> assignCapabilitiesAndSetsForPermissions(
    Collection<LoadablePermissionEntity> perms) {
    return processPermissionsByRoleId(perms, this::assignCapabilitiesAndSets);
  }

  public Set<LoadablePermissionEntity> removeCapabilitiesAndSetsForPermissions(
    Collection<LoadablePermissionEntity> perms) {
    return processPermissionsByRoleId(perms, this::removeCapabilitiesAndSets);
  }

  private Stream<LoadablePermissionEntity> assignCapabilitiesAndSets(UUID roleId,
    List<LoadablePermissionEntity> permissions) {
    assert roleId != null;

    log.debug("Assigning capabilities/capability sets for role: roleId = {}", roleId);

    var changed = new ArrayList<LoadablePermissionEntity>();
    var permsByName = permissionsByName(permissions);

    assignCapabilitiesToPermissions(roleId, permsByName, changed);
    assignCapabilitySetsToPermissions(roleId, permsByName, changed);

    log.debug("Capabilities/capability sets assigned: permissions = {}", changed);
    return changed.stream();
  }

  private Stream<LoadablePermissionEntity> removeCapabilitiesAndSets(UUID roleId,
    List<LoadablePermissionEntity> permissions) {
    assert roleId != null;

    log.debug("Removing capabilities/capability sets from role: roleId = {}", roleId);

    var capabilityIds = new ArrayList<UUID>();
    var capabilitySetIds = new ArrayList<UUID>();
    var changed = new ArrayList<LoadablePermissionEntity>();

    for (LoadablePermissionEntity perm : permissions) {
      boolean hasChanges = collectIdsAndClearPermission(perm, capabilityIds, capabilitySetIds);
      if (hasChanges) {
        changed.add(perm);
      }
    }

    roleCapabilityService.delete(roleId, capabilityIds);
    roleCapabilitySetService.delete(roleId, capabilitySetIds);

    log.debug("Capabilities/capability sets removed: permissions = {}", changed);
    return changed.stream();
  }

  private void assignCapabilitiesToPermissions(UUID roleId, Map<String, LoadablePermissionEntity> permsByName,
    List<LoadablePermissionEntity> changed) {
    var capabilities = capabilityService.findByPermissionNamesNoTechnical(permsByName.keySet());

    if (isNotEmpty(capabilities)) {
      roleCapabilityService.create(roleId, mapItems(capabilities, Capability::getId), false);

      for (Capability cap : capabilities) {
        var perm = permsByName.get(cap.getPermission());
        perm.setCapabilityId(cap.getId());
        changed.add(perm);
      }
    }
  }

  private void assignCapabilitySetsToPermissions(UUID roleId, Map<String, LoadablePermissionEntity> permsByName,
    List<LoadablePermissionEntity> changed) {
    var capabilitySets = capabilitySetService.findByPermissionNames(permsByName.keySet());

    if (isNotEmpty(capabilitySets)) {
      roleCapabilitySetService.create(roleId, mapItems(capabilitySets, CapabilitySet::getId), false);

      for (CapabilitySet set : capabilitySets) {
        var perm = permsByName.get(set.getPermission());
        perm.setCapabilitySetId(set.getId());
        changed.add(perm);
      }
    }
  }

  private boolean collectIdsAndClearPermission(LoadablePermissionEntity perm, List<UUID> capabilityIds,
    List<UUID> capabilitySetIds) {
    boolean hasChanges = false;

    if (perm.getCapabilityId() != null) {
      capabilityIds.add(perm.getCapabilityId());
      perm.setCapabilityId(null);
      hasChanges = true;
    }

    if (perm.getCapabilitySetId() != null) {
      capabilitySetIds.add(perm.getCapabilitySetId());
      perm.setCapabilitySetId(null);
      hasChanges = true;
    }

    return hasChanges;
  }

  private Set<LoadablePermissionEntity> processPermissionsByRoleId(Collection<LoadablePermissionEntity> perms,
    BiFunction<UUID, List<LoadablePermissionEntity>, Stream<LoadablePermissionEntity>> processor) {
    return groupPermissionsByRoleId(perms)
      .flatMap(entry -> processor.apply(entry.getKey(), entry.getValue()))
      .collect(toSet());
  }

  private static Stream<Entry<UUID, List<LoadablePermissionEntity>>> groupPermissionsByRoleId(
    Collection<LoadablePermissionEntity> perms) {
    return toStream(perms).collect(groupingBy(LoadablePermissionEntity::getRoleId)).entrySet().stream();
  }

  private static Map<String, LoadablePermissionEntity> permissionsByName(List<LoadablePermissionEntity> perms) {
    return toStream(perms).collect(toMap(LoadablePermissionEntity::getPermissionName, identity()));
  }
}
