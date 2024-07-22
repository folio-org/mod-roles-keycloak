package org.folio.roles.service.loadablerole;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.dto.LoadableRoleType.DEFAULT;
import static org.folio.roles.domain.entity.LoadableRoleEntity.DEFAULT_LOADABLE_ROLE_SORT;
import static org.folio.roles.service.ServiceUtils.comparatorById;
import static org.folio.roles.service.ServiceUtils.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Supplier;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.roles.service.ServiceUtils.UpdatePair;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Log4j2
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LoadableRoleService {

  private final LoadableRoleRepository repository;
  private final LoadableRoleMapper mapper;
  private final KeycloakRoleService keycloakService;
  private final TransactionTemplate trx;

  public LoadableRoles find(String query, Integer limit, Integer offset) {
    var rolePage = findByQuery(query, limit, offset).map(mapper::toRole);

    return mapper.toLoadableRoles(rolePage);
  }

  public Optional<LoadableRole> findByIdOrName(UUID id, String name) {
    return repository.findByIdOrName(id, name)
      .map(mapper::toRole);
  }

  public boolean isDefaultRole(UUID id) {
    return repository.existsByIdAndType(id, EntityLoadableRoleType.DEFAULT);
  }

  public int defaultRoleCount() {
    return repository.countAllByType(EntityLoadableRoleType.DEFAULT);
  }

  /**
   * Delete all default roles in Keycloak. Deletes only data in Keycloak.
   */
  public void cleanupDefaultRolesFromKeycloak() {
    try (var loadableRoles = repository.findAllByType(EntityLoadableRoleType.DEFAULT)) {
      loadableRoles.map(LoadableRoleEntity::getName)
        .map(keycloakService::findByName)
        .forEach(optRole -> optRole.ifPresent(kcRole -> keycloakService.deleteByIdSafe(kcRole.getId())));
    }
  }

  @Transactional
  public LoadableRole save(LoadableRole role) {
    var entity = mapper.toRoleEntity(role);

    var saved = hasRole(entity) ? updateRole(entity) : createRole(entity);
    return mapper.toRole(saved);
  }

  public void saveAll(List<LoadableRole> roles) {
    var existing = findByQuery("type==" + DEFAULT.getValue(), MAX_VALUE, 0).getContent();
    var incoming = mapper.toRoleEntity(roles);

    var created = new ArrayList<LoadableRoleEntity>();
    var updated = new ArrayList<UpdatePair<LoadableRoleEntity>>();
    var deleted = new ArrayList<LoadableRoleEntity>();
    merge(incoming, existing, comparatorById(),
      created::add, updated::add, deleted::add);

    deleteAll(deleted);
    createAll(created);
    updateAll(updated);
  }

  private Page<LoadableRoleEntity> findByQuery(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_LOADABLE_ROLE_SORT);
    return repository.findByQuery(query, offsetRequest);
  }

  private boolean hasRole(LoadableRoleEntity role) {
    return role.getId() != null && repository.existsById(role.getId());
  }

  private LoadableRoleEntity createRole(LoadableRoleEntity entity) {
    var role = mapper.toRegularRole(entity);
    // role id populate inside the method, as a side effect, and the original object returned
    var roleWithId = keycloakService.create(role);
    entity.setId(roleWithId.getId());

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

  private List<LoadableRoleEntity> createAll(List<LoadableRoleEntity> entities) {
    if (isEmpty(entities)) {
      log.debug("No loadable roles to create");
      return entities;
    }

    var createdInKeycloakRoleIds = new ArrayList<UUID>();
    try {
      entities.forEach(entity -> {
        var role = mapper.toRegularRole(entity);
        // role id populate inside the method, as a side effect, and the original object returned
        var roleId = keycloakService.create(role).getId();
        entity.setId(roleId);

        createdInKeycloakRoleIds.add(roleId);
      });

      var result = trx.execute(status -> repository.saveAll(entities));
      log.info("Loadable roles created: {}", toIdNames(result));

      return result;
    } catch (Exception e) {
      createdInKeycloakRoleIds.forEach(keycloakService::deleteByIdSafe);

      throw new ServiceException("Failed to create loadable roles", e);
    }
  }

  private void updateAll(List<UpdatePair<LoadableRoleEntity>> updatePairs) {
    if (isEmpty(updatePairs)) {
      log.debug("No loadable roles to update");
      return;
    }

    updateNameAndDescription(updatePairs);
    updatePermissions(updatePairs);
  }

  private void updateNameAndDescription(List<UpdatePair<LoadableRoleEntity>> updatePairs) {
    var changedRoles = updatePairs.stream()
      .filter(isRoleDataChanged())
      .map(copyNameAndDescription()).toList();

    if (isNotEmpty(changedRoles)) {
      trx.executeWithoutResult(status -> {
        repository.saveAllAndFlush(changedRoles);

        changedRoles.forEach(changed -> keycloakService.update(mapper.toRegularRole(changed)));
      });

      log.info("Loadable role name(s)/description(s) updated: {}", toIdNames(changedRoles));
    }
  }

  private void updatePermissions(List<UpdatePair<LoadableRoleEntity>> updatePairs) {

  }

  private void deleteAll(List<LoadableRoleEntity> entities) {
    if (isEmpty(entities)) {
      log.debug("No loadable roles to delete");
      return;
    }

    trx.executeWithoutResult(status -> {
      repository.flush();
      repository.deleteAllInBatch(entities);

      entities.forEach(entity -> keycloakService.deleteById(entity.getId()));
    });
    log.info("Loadable roles deleted: {}", toIdNames(entities));
  }

  private LoadableRoleEntity saveToDb(LoadableRoleEntity entity) {
    assert entity.getId() != null;

    return repository.save(entity);
  }

  private static Predicate<UpdatePair<LoadableRoleEntity>> isRoleDataChanged() {
    return pair -> {
      var newItem = pair.newItem();
      var oldItem = pair.oldItem();

      return !oldItem.equalsLogically(newItem);
    };
  }

  private static Function<UpdatePair<LoadableRoleEntity>, LoadableRoleEntity> copyNameAndDescription() {
    return pair -> {
      var newItem = pair.newItem();
      var oldItem = pair.oldItem();

      oldItem.setName(newItem.getName());
      oldItem.setDescription(newItem.getDescription());

      return oldItem;
    };
  }

  private static Supplier<List<String>> toIdNames(List<LoadableRoleEntity> result) {
    return () -> mapItems(result, entity -> format("[roleId = %s, roleName = %s]", entity.getId(), entity.getName()));
  }
}
