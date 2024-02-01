package org.folio.roles.service.permission;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.startsWithAny;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.Collectors.toLinkedHashMap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.folio.common.utils.CollectionUtils;
import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.mapper.entity.PermissionEntityMapper;
import org.folio.roles.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FolioPermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionEntityMapper permissionEntityMapper;

  /**
   * Expands permission names including all backend subPermission entities.
   *
   * @param permissionNames - list of {@link String} permission names to expand
   * @return a flat list with expanded {@link Permission} objects
   */
  @Transactional(readOnly = true)
  public List<Permission> expandPermissionNames(Collection<String> permissionNames) {
    if (isEmpty(permissionNames)) {
      return emptyList();
    }

    var foundPermissions = new LinkedHashSet<String>();
    var foundEntities = new LinkedHashSet<PermissionEntity>();
    var currPermissionNames = getAsSetOfStrings(permissionNames);

    do {
      var foundPermissionEntities = permissionRepository.findByPermissionNameIn(currPermissionNames);
      foundPermissions.addAll(mapItems(foundPermissionEntities, PermissionEntity::getPermissionName));
      foundEntities.addAll(foundPermissionEntities);
      currPermissionNames = getSubPermissionNames(foundPermissionEntities, foundPermissions);
    } while (isNotEmpty(currPermissionNames));

    return permissionEntityMapper.toDto(foundEntities);
  }

  /**
   * Create folio permission records ignoring conflicts.
   *
   * <ul>
   *   <li>If permission already exists by name - a new entity will be ignored</li>
   * </ul>
   *
   * @param permissions - list with {@link Permission} records to create
   * @return {@link List} with created {@link Permission} records
   */
  @Transactional
  public PageResult<Permission> createIgnoringConflicts(List<Permission> permissions) {
    if (isEmpty(permissions)) {
      return PageResult.empty();
    }

    var permissionsByName = permissions.stream().collect(toLinkedHashMap(Permission::getPermissionName));
    var foundEntities = permissionRepository.findByPermissionNameIn(permissionsByName.keySet());
    foundEntities.forEach(permission -> permissionsByName.remove(permission.getPermissionName()));

    var entitiesToCreate = mapItems(permissionsByName.values(), permissionEntityMapper::toEntity);
    var createdEntities = permissionRepository.saveAll(entitiesToCreate);
    var totalRecords = foundEntities.size() + createdEntities.size();
    var union = union(permissionEntityMapper.toDto(foundEntities), permissionEntityMapper.toDto(createdEntities));
    return PageResult.of(totalRecords, union);
  }

  private static Set<String> getSubPermissionNames(List<PermissionEntity> entities, Set<String> foundPermissions) {
    return entities.stream()
      .map(PermissionEntity::getSubPermissions)
      .flatMap(CollectionUtils::toStream)
      .filter(not(foundPermissions::contains))
      .collect(toSet());
  }

  private static Set<String> getAsSetOfStrings(Collection<String> permissionNames) {
    return permissionNames instanceof Set ? (Set<String>) permissionNames : new LinkedHashSet<>(permissionNames);
  }

  private static boolean isUiPermission(String permissionName) {
    return startsWithAny(permissionName, "ui-", "module", "plugin", "settings");
  }
}
