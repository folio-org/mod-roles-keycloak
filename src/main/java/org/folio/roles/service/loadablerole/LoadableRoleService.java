package org.folio.roles.service.loadablerole;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.domain.entity.LoadableRoleEntity.DEFAULT_LOADABLE_ROLE_SORT;
import static org.folio.roles.service.ServiceUtils.comparatorById;
import static org.folio.roles.service.ServiceUtils.merge;
import static org.folio.roles.service.ServiceUtils.mergeInBatch;
import static org.folio.roles.service.ServiceUtils.nothing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.roles.service.ServiceUtils.UpdatePair;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class LoadableRoleService {

  private final LoadableRoleRepository repository;
  private final LoadableRoleMapper mapper;
  private final KeycloakRoleService keycloakService;
  private final LoadableRoleCapabilityAssignmentHelper assignmentHelper;
  private final LoadableRoleAsyncAssignmentRetryer loadableRoleAsyncAssignmentRetryer;

  @Transactional(readOnly = true)
  public LoadableRoles find(String query, Integer limit, Integer offset) {
    var rolePage = findByQuery(query, limit, offset).map(mapper::toRole);

    return mapper.toLoadableRoles(rolePage);
  }

  @Transactional(readOnly = true)
  public Optional<LoadableRole> findByIdOrName(UUID id, String name) {
    return repository.findByIdOrName(id, name)
      .map(mapper::toRole);
  }

  @Transactional(readOnly = true)
  public boolean isDefaultRole(UUID id) {
    return repository.existsByIdAndType(id, EntityRoleType.DEFAULT);
  }

  /**
   * Delete all default roles in Keycloak. Deletes only data in Keycloak.
   */
  @Transactional(readOnly = true)
  public void cleanupDefaultRolesFromKeycloak() {
    try (var loadableRoles = repository.findAllByType(EntityRoleType.DEFAULT)) {
      loadableRoles.map(LoadableRoleEntity::getName)
        .map(keycloakService::findByName)
        .forEach(optRole -> optRole.ifPresent(kcRole -> deleteByIdSafe(kcRole.getId())));
    }
  }

  @Transactional
  public LoadableRole save(LoadableRole role) {
    var entity = mapper.toRoleEntity(role);

    var saved = saveOrUpdate(entity);
    return mapper.toRole(saved);
  }

  @Transactional
  public void saveAll(List<LoadableRole> roles) {
    toStream(roles).collect(groupingBy(LoadableRole::getType))
      .forEach(this::saveAllByType);
  }

  @Transactional
  public LoadableRole upsertDefaultLoadableRole(LoadableRole loadableRole) {
    prepareLoadableRole(loadableRole);
    var incoming = mapper.toRoleEntity(loadableRole);
    var existing = repository.findByIdOrName(loadableRole.getId(), loadableRole.getName())
      .map(List::of)
      .orElse(List.of());
    mergeInBatch(List.of(incoming), existing, comparatorById(), this::createAll, this::updateAll, nothing());

    var saved = repository.findByIdOrName(loadableRole.getId(), loadableRole.getName())
      .orElseThrow(() -> new ServiceException("Loadable role not found in DB"));

    var isPermissionNotAssignedExist = toStream(saved.getPermissions())
      .anyMatch(per -> per.getCapabilityId() == null);
    if (isPermissionNotAssignedExist) {
      loadableRoleAsyncAssignmentRetryer.retryAssignCapabilitiesAndSetsForPermissions(saved.getId(), saved.getName());
    }

    return mapper.toRole(saved);
  }

  /**
   * Deletes role by identifier, suppressing all exception during delete process.
   *
   * @param id - role identifier
   */
  private void deleteByIdSafe(UUID id) {
    try {
      keycloakService.deleteById(id);
    } catch (Exception exception) {
      log.debug("Failed to delete Role in Keycloak: id = {}", id, exception);
    }
  }

  private void prepareLoadableRole(LoadableRole loadableRole) {
    repository.findByIdOrName(loadableRole.getId(), loadableRole.getName())
      .ifPresent(found -> loadableRole.setId(found.getId()));
    loadableRole.getPermissions().forEach(p -> p.setRoleId(loadableRole.getId()));
    loadableRole.setType(DEFAULT);
  }

  private LoadableRoleEntity saveOrUpdate(LoadableRoleEntity entity) {
    return hasRole(entity) ? updateRole(entity) : createRole(entity);
  }

  private Page<LoadableRoleEntity> findByQuery(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_LOADABLE_ROLE_SORT);
    return repository.findByQuery(query, offsetRequest);
  }

  private boolean hasRole(LoadableRoleEntity role) {
    var id = role.getId();
    return id != null && repository.existsById(id);
  }

  private LoadableRoleEntity createRole(LoadableRoleEntity entity) {
    var role = mapper.toRegularRole(entity);
    // role id populate inside the method, as a side effect, and the original object returned
    var roleWithId = keycloakService.create(role);
    updateRoleId(entity, roleWithId.getId());

    try {
      var created = saveToDb(entity);
      log.info("Loadable role has been created: id = {}, name = {}", created.getId(), created.getName());

      return created;
    } catch (Exception exception) {
      keycloakService.deleteById(entity.getId());
      throw new ServiceException("Failed to create loadable role", exception);
    }
  }

  private LoadableRoleEntity updateRole(LoadableRoleEntity entity) {
    var saved = saveToDb(entity);
    keycloakService.update(mapper.toRegularRole(saved));

    log.info("Loadable role has been updated: id = {}, name = {}", saved.getId(), saved.getName());
    return saved;
  }

  private void saveAllByType(RoleType type, List<LoadableRole> roles) {
    requireNonNull(type);

    if (type == DEFAULT) {
      saveDefaultRoles(roles);
    } else {
      roles.forEach(this::save);
    }
  }

  private void saveDefaultRoles(List<LoadableRole> roles) {
    var existing = findAllDefaultRolesLoadedFromFiles();
    var incoming = mapper.toRoleEntity(roles);
    incoming.forEach(role -> role.setLoadedFromFile(true));
    log.debug("Saving default roles:\n\texisting = {},\n\tincoming = {}", () -> toIdNames(existing),
      () -> toIdNames(incoming));

    mergeInBatch(incoming, existing, comparatorById(), this::createAll, this::updateAll, this::deleteAll);
  }

  private List<LoadableRoleEntity> findAllDefaultRolesLoadedFromFiles() {
    try (var defaultRoles = repository.findAllByTypeAndLoadedFromFile(EntityRoleType.DEFAULT, true)) {
      return defaultRoles.toList();
    }
  }

  private Collection<LoadableRoleEntity> createAll(Collection<LoadableRoleEntity> entities) {
    if (isEmpty(entities)) {
      log.debug("No loadable roles to create");
      return entities;
    }

    var createdInKeycloakRoleIds = new ArrayList<UUID>();
    try {
      for (var entity : entities) {
        var role = mapper.toRegularRole(entity);

        var roleId = keycloakService.findByName(role.getName())
          .orElseGet(() -> keycloakService.create(role))
          .getId();
        updateRoleId(entity, roleId);

        createdInKeycloakRoleIds.add(roleId);
        repository.saveAndFlush(entity);

        assignmentHelper.assignCapabilitiesAndSetsForPermissions(entity.getPermissions());
      }

      var result = repository.saveAll(entities);
      log.info("Loadable roles created: {}", () -> toIdNames(result));

      return result;
    } catch (Exception e) {
      createdInKeycloakRoleIds.forEach(this::deleteByIdSafe);

      throw new ServiceException("Failed to create loadable roles", e);
    }
  }

  private void updateAll(Collection<UpdatePair<LoadableRoleEntity>> updatePairs) {
    if (isEmpty(updatePairs)) {
      log.debug("No loadable roles to update");
      return;
    }

    var changedRoles = new ArrayList<LoadableRoleEntity>();
    for (var pair : updatePairs) {
      var incoming = pair.newItem();
      var existing = pair.oldItem();

      var nameDescriptionUpdated = updateNameAndDescription(incoming, existing);
      var permsUpdated = updatePermissions(incoming, existing);
      if (nameDescriptionUpdated || permsUpdated) {
        changedRoles.add(existing);
      }
    }

    repository.saveAll(changedRoles);
    log.info("Loadable roles updated: {}", () -> toIdNames(changedRoles));
  }

  private boolean updateNameAndDescription(LoadableRoleEntity source, LoadableRoleEntity target) {
    var modified = false;

    if (isRoleDataChanged(target, source)) {
      target.setName(source.getName());
      target.setDescription(source.getDescription());
      target.setLoadedFromFile(source.isLoadedFromFile());

      keycloakService.update(mapper.toRegularRole(target));
      log.debug("Loadable role name/description updated: roleId = {}, name = {}, description = {}",
        target.getId(), target.getName(), target.getDescription());

      modified = true;
    }
    return modified;
  }

  private boolean updatePermissions(LoadableRoleEntity source, LoadableRoleEntity target) {
    var existingPerms = target.getPermissions();
    var incomingPerms = source.getPermissions();

    var created = new LinkedHashSet<LoadablePermissionEntity>();
    var deleted = new LinkedHashSet<LoadablePermissionEntity>();
    merge(incomingPerms, existingPerms, comparatorById(), created::add, nothing(), deleted::add);
    log.debug("Merge result for default role permissions: roleId = {}, created = {}, deleted = {}",
      target::getId, () -> toNames(created), () -> toNames(deleted));

    assignmentHelper.assignCapabilitiesAndSetsForPermissions(created);
    created.forEach(target::addPermission);

    assignmentHelper.removeCapabilitiesAndSetsForPermissions(deleted);
    deleted.forEach(target::removePermission);

    return isNotEmpty(created) || isNotEmpty(deleted);
  }

  private void deleteAll(Collection<LoadableRoleEntity> entities) {
    if (isEmpty(entities)) {
      log.debug("No loadable roles to delete");
      return;
    }

    repository.flush();
    repository.deleteAllInBatch(entities);

    entities.forEach(entity -> deleteByIdSafe(entity.getId()));
    log.info("Loadable roles deleted: {}", () -> toIdNames(entities));
  }

  private LoadableRoleEntity saveToDb(LoadableRoleEntity entity) {
    requireNonNull(entity.getId(), "Loadable role id cannot be null");

    return repository.save(entity);
  }

  private static void updateRoleId(LoadableRoleEntity entity, UUID roleId) {
    entity.setId(roleId);
    entity.getPermissions().forEach(perm -> perm.setRoleId(roleId));
  }

  private static boolean isRoleDataChanged(LoadableRoleEntity first, LoadableRoleEntity second) {
    return !first.equalsLogically(second);
  }

  private static List<String> toIdNames(Collection<LoadableRoleEntity> roles) {
    return mapItems(roles, entity -> format("[roleId = %s, roleName = %s]", entity.getId(), entity.getName()));
  }

  private static List<String> toNames(Collection<LoadablePermissionEntity> perms) {
    return mapItems(perms, LoadablePermissionEntity::getPermissionName);
  }
}
