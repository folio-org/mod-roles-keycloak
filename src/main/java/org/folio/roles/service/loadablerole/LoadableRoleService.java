package org.folio.roles.service.loadablerole;

import static org.folio.roles.domain.entity.type.EntityLoadableRoleType.DEFAULT;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.mapper.entity.LoadableRoleEntityMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class LoadableRoleService {

  private final LoadableRoleRepository repository;
  private final LoadableRoleEntityMapper mapper;
  private final KeycloakRoleService keycloakService;

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

  public LoadableRole save(LoadableRole role) {
    return hasRole(role) ? updateRole(role) : createRole(role);
  }

  private boolean hasRole(LoadableRole role) {
    return role.getId() != null && repository.existsById(role.getId());
  }

  private LoadableRole createRole(LoadableRole role) {
    // role can be present in Keycloak, so trying to find the one by the name first
    var roleWithId = keycloakService.findByName(role.getName())
      .map(found -> copyDataFrom(found, role))
      // role id populate inside the method, as a side effect, and the original object returned
      .orElseGet(() -> (LoadableRole) keycloakService.create(role));
    
    try {
      var created = saveToDb(roleWithId);
      log.info("Loadable role has been created: id = {}, name = {}", created.getId(), created.getName());

      return created;
    } catch (Exception exception) {
      keycloakService.deleteById(roleWithId.getId());
      throw new ServiceException("Failed to create loadable role", "cause", exception.getMessage());
    }
  }

  private LoadableRole updateRole(LoadableRole role) {
    var saved = saveToDb(role);
    keycloakService.update(saved);

    log.info("Loadable role has been updated: id = {}, name = {}", role.getId(), role.getName());

    return saved;
  }

  private LoadableRole saveToDb(LoadableRole role) {
    assert role.getId() != null;

    var entity = mapper.toRoleEntity(role);
    var savedEntity = repository.save(entity);

    return mapper.toRole(savedEntity);
  }

  private static LoadableRole copyDataFrom(Role role, LoadableRole loadableRole) {
    return loadableRole
      .id(role.getId())
      .name(role.getName())
      .description(role.getDescription());
  }
}
