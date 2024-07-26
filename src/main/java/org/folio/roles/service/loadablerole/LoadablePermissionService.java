package org.folio.roles.service.loadablerole;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class LoadablePermissionService {

  private final LoadablePermissionRepository repository;
  private final LoadableRoleMapper mapper;
  private final CapabilitySetService capabilitySetService;
  private final CapabilityService capabilityService;
  private final RoleCapabilityService roleCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;

  @Transactional(readOnly = true)
  public List<LoadablePermission> findAllByPermissions(Collection<String> permissionNames) {
    var entities = repository.findAllByPermissionNameIn(permissionNames);
    return mapper.toPermission(entities);
  }

  public LoadablePermission save(LoadablePermission perm) {
    var entity = mapper.toPermissionEntity(perm);
    var saved = repository.save(entity);

    return mapper.toPermission(saved);
  }

  public List<LoadablePermission> saveAll(List<LoadablePermission> perms) {
    var entities = mapper.toPermissionEntity(perms);
    var saved = repository.saveAll(entities);

    return mapper.toPermission(saved);
  }

  public void assignRoleCapabilitiesAndSetsForPermissions(List<LoadablePermission> perms) {
    var changed = groupPermissionsByRoleId(perms)
      .flatMap(permsByRoleId -> assignCapabilitiesAndSets(permsByRoleId.getKey(), permsByRoleId.getValue()))
      .toList();

    saveAll(changed);
  }

  public void removeRoleCapabilitiesAndSetsForPermissions(List<LoadablePermission> perms) {
    var changed = groupPermissionsByRoleId(perms)
      .flatMap(permsByRoleId -> removeCapabilitiesAndSets(permsByRoleId.getKey(), permsByRoleId.getValue()))
      .toList();

    saveAll(changed);
  }

  private Stream<LoadablePermission> assignCapabilitiesAndSets(UUID roleId, List<LoadablePermission> permissions) {
    if (roleId == null) {
      throw new IllegalArgumentException("Role id cannot be null");
    }

    var changed = new ArrayList<LoadablePermission>();

    var permsByName = permissionsByName(permissions);

    var capabilities = capabilityService.findByPermissionNames(permsByName.keySet());
    if (isNotEmpty(capabilities)) {
      roleCapabilityService.create(roleId, mapItems(capabilities, Capability::getId));

      for (Capability cap : capabilities) {
        var perm = permsByName.get(cap.getPermission());
        perm.setCapabilityId(cap.getId());
        changed.add(perm);
      }
    }

    var capabilitySets = capabilitySetService.findByPermissionNames(permsByName.keySet());
    if (isNotEmpty(capabilitySets)) {
      roleCapabilitySetService.create(roleId, mapItems(capabilitySets, CapabilitySet::getId));

      for (CapabilitySet set : capabilitySets) {
        var perm = permsByName.get(set.getPermission());
        perm.setCapabilitySetId(set.getId());
        changed.add(perm);
      }
    }

    return changed.stream();
  }

  private Stream<LoadablePermission> removeCapabilitiesAndSets(UUID roleId, List<LoadablePermission> permissions) {
    if (roleId == null) {
      throw new IllegalArgumentException("Role id cannot be null");
    }

    var changed = new ArrayList<LoadablePermission>();

    var capabilityIds = new ArrayList<UUID>();
    var capabilitySetIds = new ArrayList<UUID>();

    for (LoadablePermission perm : permissions) {
      if (perm.getCapabilityId() != null) {
        capabilityIds.add(perm.getCapabilityId());
      }

      if (perm.getCapabilitySetId() != null) {
        capabilitySetIds.add(perm.getCapabilitySetId());
      }

      if (perm.getCapabilityId() != null || perm.getCapabilitySetId() != null) {
        perm.setCapabilityId(null);
        perm.setCapabilitySetId(null);
        changed.add(perm);
      }
    }

    roleCapabilityService.delete(roleId, capabilityIds);
    roleCapabilitySetService.delete(roleId, capabilitySetIds);

    return changed.stream();
  }

  private static Stream<Entry<UUID, List<LoadablePermission>>> groupPermissionsByRoleId(
    List<LoadablePermission> perms) {
    return toStream(perms).collect(groupingBy(LoadablePermission::getRoleId)).entrySet().stream();
  }

  private static Map<String, LoadablePermission> permissionsByName(List<LoadablePermission> perms) {
    return toStream(perms).collect(toMap(LoadablePermission::getPermissionName, identity()));
  }
}
