package org.folio.roles.service.role;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.folio.common.utils.CollectionUtils.mapItems;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Log4j2
@Service
@RequiredArgsConstructor
public class RoleService {

  private final KeycloakRoleService keycloakService;
  private final RoleEntityService entityService;

  /**
   * Find one role by ID.
   *
   * @param id - role identifier
   * @return existing role
   */
  @Transactional(readOnly = true)
  public Role getById(UUID id) {
    return entityService.getById(id);
  }

  /**
   * Find by ids.
   *
   * @param ids - role identifiers
   * @return existing role
   */
  @Transactional(readOnly = true)
  public List<Role> findByIds(List<UUID> ids) {
    var roles = entityService.findByIds(ids);
    if (roles.size() != ids.size()) {
      var notFoundIds = ListUtils.subtract(ids, mapItems(roles, Role::getId));
      throw new EntityNotFoundException("Roles are not found for ids: " + notFoundIds);
    }

    return roles;
  }

  /**
   * Search roles by query or/and offset or/and limit. Searching without parameters returns all roles.
   *
   * @param query  - query for searching roles
   * @param offset - offset for searching roles
   * @param limit  - limit for searching roles
   * @return {@link Roles} which contains array of {@link Role} and number of elements in the roles array
   */
  @Transactional(readOnly = true)
  public Roles search(String query, Integer offset, Integer limit) {
    var roles = entityService.findByQuery(query, offset, limit);
    return buildRoles(roles);
  }

  /**
   * Create a role.
   *
   * @param role - role object for creating
   * @return created {@link Role} object
   * @throws RequestValidationException if some of the provided fields are invalid
   */
  @Transactional
  public Role create(Role role) {
    var createdRole = keycloakService.create(role);
    try {
      return entityService.create(createdRole);
    } catch (Exception exception) {
      keycloakService.deleteById(createdRole.getId());
      throw new ServiceException("Failed to create role", "cause", exception.getMessage());
    }
  }

  /**
   * Create one or more roles.
   *
   * @param roles - roles for creating
   * @return {@link Roles} which contains array of {@link Role} and number of elements in the roles array
   */
  @Transactional
  public Roles create(List<Role> roles) {
    var createdRoles = roles.stream()
      .map(this::createSafe)
      .flatMap(Optional::stream)
      .collect(toList());
    return buildRoles(createdRoles);
  }

  /**
   * Update one role.
   *
   * @param role - role for updating
   * @return updated role {@link Role}
   */
  @Transactional
  public Role update(Role role) {
    Assert.notNull(role.getId(), "Role should has ID");
    var actualRole = keycloakService.getById(role.getId());
    keycloakService.update(role);
    try {
      return entityService.update(role);
    } catch (Exception e) {
      log.debug("Rollback role state in Keycloak: id = {}, name = {}", actualRole.getId(), actualRole.getName());
      keycloakService.update(actualRole);
      throw e;
    }
  }

  /**
   * Delete role by ID.
   *
   * @param id - role identifier
   */
  @Transactional
  public void deleteById(UUID id) {
    var actualRole = keycloakService.getById(id);
    entityService.deleteById(id);
    try {
      keycloakService.deleteById(id);
    } catch (Exception e) {
      log.debug("Rollback deleted policy in db: id = {}, name = {}", actualRole.getId(), actualRole.getName());
      entityService.create(actualRole);
      throw e;
    }
  }

  private Optional<Role> createSafe(Role role) {
    var createdRole = keycloakService.createSafe(role);
    if (createdRole.isEmpty()) {
      return empty();
    }
    try {
      return of(entityService.create(createdRole.get()));
    } catch (Exception e) {
      keycloakService.deleteById(createdRole.get().getId());
      log.debug("Rollback created in Keycloak role: name = {}", createdRole.get().getName(), e);
      return empty();
    }
  }

  private Roles buildRoles(List<Role> roles) {
    return new Roles().roles(roles).totalRecords(roles.size());
  }
}
