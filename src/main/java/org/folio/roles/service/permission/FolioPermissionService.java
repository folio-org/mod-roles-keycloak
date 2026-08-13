package org.folio.roles.service.permission;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.Collectors.toLinkedHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.utils.CollectionUtils;
import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.mapper.entity.PermissionEntityMapper;
import org.folio.roles.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class FolioPermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionEntityMapper permissionEntityMapper;

  /**
   * Expands permission names including all reachable subPermission names.
   *
   * <p>A name without a stored permission record is kept in the result: the module owning it may not
   * have been processed yet, and the declared hierarchy remains the source of truth. Dropping such a
   * name would remove it from every capability set that holds it indirectly.</p>
   *
   * @param permissionNames - list of {@link String} permission names to expand
   * @return a flat list with expanded permission names
   */
  @Transactional(readOnly = true)
  public List<String> expandPermissionNames(Collection<String> permissionNames) {
    if (isEmpty(permissionNames)) {
      return emptyList();
    }

    var expandedNames = new LinkedHashSet<String>();
    var currPermissionNames = getAsSetOfStrings(permissionNames);

    do {
      expandedNames.addAll(currPermissionNames);
      var foundPermissionEntities = permissionRepository.findByPermissionNameIn(currPermissionNames);
      currPermissionNames = getSubPermissionNames(foundPermissionEntities, expandedNames);
    } while (isNotEmpty(currPermissionNames));

    return List.copyOf(expandedNames);
  }

  /**
   * Create folio permission records ignoring conflicts.
   *
   * <ul>
   *   <li>If permission already exists by name - a new entity will be ignored</li>
   * </ul>
   *
   * @param newPermissions - list with {@link Permission} records to create
   * @param oldPermissions - list with {@link Permission} records to compare and remove deprecated values
   */
  @Transactional
  public void update(List<Permission> newPermissions, List<Permission> oldPermissions) {
    var permissionsByName = groupByPermissionName(newPermissions);
    var oldPermissionsByName = groupByPermissionName(oldPermissions);

    var deprecatedPermissions = oldPermissionsByName.entrySet().stream()
      .filter(not(entry -> permissionsByName.containsKey(entry.getKey())))
      .map(Entry::getValue)
      .toList();

    upsertNewPermissions(newPermissions);
    removeDeprecatedPermissions(deprecatedPermissions);
  }

  private void upsertNewPermissions(List<Permission> newPermissions) {
    if (isEmpty(newPermissions)) {
      return;
    }

    var newPermissionNames = mapItems(newPermissions, Permission::getPermissionName);

    var existingPermissionEntities = permissionRepository.findByPermissionNameIn(newPermissionNames);
    var existingPermissionEntitiesDeduplicated = new ArrayList<PermissionEntity>(existingPermissionEntities.size());
    var existingPermissionEntitiesNames = new HashSet<String>();
    existingPermissionEntities.forEach(existingEntity -> {
      if (existingPermissionEntitiesNames.contains(existingEntity.getPermissionName())) {
        // Duplicate found
        log.info("Removing duplicate permission {} with ID {}", existingEntity.getPermissionName(),
          existingEntity.getId());
        permissionRepository.delete(existingEntity);
      } else {
        existingPermissionEntitiesNames.add(existingEntity.getPermissionName());
        existingPermissionEntitiesDeduplicated.add(existingEntity);
      }
    });

    var foundEntityIdsMap = existingPermissionEntitiesDeduplicated.stream()
      .collect(toMap(PermissionEntity::getPermissionName, PermissionEntity::getId));

    var entitiesToCreate = mapItems(newPermissions, permissionEntityMapper::toEntity);
    for (var entity : entitiesToCreate) {
      entity.setId(foundEntityIdsMap.get(entity.getPermissionName()));
    }

    permissionRepository.saveAll(entitiesToCreate);
  }

  private void removeDeprecatedPermissions(List<Permission> deprecatedPermissions) {
    if (isEmpty(deprecatedPermissions)) {
      return;
    }

    var deprecatedPermissionNames = mapItems(deprecatedPermissions, Permission::getPermissionName);
    permissionRepository.deleteAllByPermissionNameIn(deprecatedPermissionNames);
  }

  private static Set<String> getSubPermissionNames(List<PermissionEntity> entities, Set<String> expandedNames) {
    return entities.stream()
      .map(PermissionEntity::getSubPermissions)
      .flatMap(CollectionUtils::toStream)
      .filter(not(expandedNames::contains))
      .collect(toSet());
  }

  private static Set<String> getAsSetOfStrings(Collection<String> permissionNames) {
    return permissionNames instanceof Set ? (Set<String>) permissionNames : new LinkedHashSet<>(permissionNames);
  }

  private static Map<String, Permission> groupByPermissionName(List<Permission> permissions) {
    return toStream(permissions).collect(toLinkedHashMap(Permission::getPermissionName));
  }
}
