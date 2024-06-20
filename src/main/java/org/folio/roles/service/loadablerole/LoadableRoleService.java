package org.folio.roles.service.loadablerole;

import static org.folio.roles.domain.entity.LoadableRoleEntity.DEFAULT_LOADABLE_ROLE_SORT;
import static org.folio.roles.domain.entity.type.EntityLoadableRoleType.DEFAULT;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class LoadableRoleService {

  private final LoadableRoleRepository repository;
  private final LoadableRoleMapper mapper;
  private final KeycloakRoleService keycloakService;

  @Transactional(readOnly = true)
  public LoadableRoles find(String query, Integer limit, Integer offset) {
    var offsetRequest = OffsetRequest.of(offset, limit, DEFAULT_LOADABLE_ROLE_SORT);
    var rolePage = repository.findByQuery(query, offsetRequest).map(mapper::toRole);

    return mapper.toLoadableRoles(rolePage);
  }

  @Transactional(readOnly = true)
  public Optional<LoadableRole> findByIdOrName(UUID id, String name) {
    return repository.findByIdOrName(id, name)
      .map(mapper::toRole);
  }

  @Transactional(readOnly = true)
  public boolean isDefaultRole(UUID id) {
    return repository.existsByIdAndType(id, DEFAULT);
  }

  @Transactional(readOnly = true)
  public int defaultRoleCount() {
    return repository.countAllByType(DEFAULT);
  }

  /**
   * Delete all default roles in Keycloak. Deletes only data in Keycloak.
   */
  @Transactional(readOnly = true)
  public void cleanupDefaultRolesFromKeycloak() {
    try (var loadableRoles = repository.findAllByType(EntityLoadableRoleType.DEFAULT)) {
      loadableRoles.map(LoadableRoleEntity::getName)
        .map(keycloakService::findByName)
        .forEach(optRole -> optRole.ifPresent(kcRole -> keycloakService.deleteByIdSafe(kcRole.getId())));
    }
  }

  public LoadableRole save(LoadableRole role) {
    return hasRole(role) ? updateRole(role) : createRole(role);
  }

  private boolean hasRole(LoadableRole role) {
    return role.getId() != null && repository.existsById(role.getId());
  }

  private LoadableRole createRole(LoadableRole loadableRole) {
    var role = mapper.toRegularRole(loadableRole);
    // role id populate inside the method, as a side effect, and the original object returned
    var roleWithId = keycloakService.create(role);
    loadableRole.setId(roleWithId.getId());

    try {
      var created = saveToDb(loadableRole);
      log.info("Loadable role has been created: id = {}, name = {}", created.getId(), created.getName());

      return created;
    } catch (Exception exception) {
      keycloakService.deleteById(loadableRole.getId());
      throw new ServiceException("Failed to create loadable role", "cause", exception.getMessage());
    }
  }

  private LoadableRole updateRole(LoadableRole role) {
    var saved = saveToDb(role);
    keycloakService.update(mapper.toRegularRole(saved));

    log.info("Loadable role has been updated: id = {}, name = {}", role.getId(), role.getName());

    return saved;
  }

  private LoadableRole saveToDb(LoadableRole role) {
    assert role.getId() != null;

    var entity = mapper.toRoleEntity(role);
    var savedEntity = repository.save(entity);

    return mapper.toRole(savedEntity);
  }
}
