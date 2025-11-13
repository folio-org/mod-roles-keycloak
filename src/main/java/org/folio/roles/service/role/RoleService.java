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
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.model.PageResult;
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
   * @param query - query for searching roles
   * @param offset - offset for searching roles
   * @param limit - limit for searching roles
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
    checkIfRoleHasDefaultType(role);
    var createdRole = keycloakService.create(role);
    try {
      createdRole.setType(role.getType());
      Role savedRole = entityService.create(createdRole);
      log.debug("Role has been created: id = {}, name = {}", savedRole.getId(), savedRole.getName());
      return savedRole;
    } catch (Exception exception) {
      keycloakService.deleteById(createdRole.getId());
      throw new ServiceException("Failed to create role", exception);
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
    var actualRole = entityService.getById(role.getId());
    checkIfRoleHasDefaultType(actualRole);
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
    var actualRole = entityService.getById(id);
    checkIfRoleHasDefaultType(actualRole);
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
    if (hasDefaultRoleType(role)) {
      log.debug("Skip role creation. Default role cannot be created via roles API: name = {}", role.getName());
      return empty();
    }
    var createdRoleOpt = createSuppressingExceptions(role);
    if (createdRoleOpt.isEmpty()) {
      // Create role entity in DB if it doesn't exist, but exists in Keycloak
      if (entityService.findByName(role.getName()).isEmpty()) {
        keycloakService.findByName(role.getName()).ifPresent(existingKeycloakRole -> {
          existingKeycloakRole.setType(role.getType());
          getOrCreateRoleEntitySafe(existingKeycloakRole);
        });
      }
      return empty();
    }
    var createdRole = createdRoleOpt.get();
    createdRole.setType(role.getType());
    return getOrCreateRoleEntitySafe(createdRole);
  }

  /**
   * Creates a role, suppressing all exception during create process.
   *
   * @param role - role object to be created
   * @return {@link Optional} with created {@link Role}, or {@link Optional#empty()} if exception occurred
   */
  private Optional<Role> createSuppressingExceptions(Role role) {
    try {
      return Optional.of(keycloakService.create(role));
    } catch (Exception e) {
      log.debug("Failed to create role: name = {}", role.getName(), e);
      return empty();
    }
  }

  private Optional<Role> getOrCreateRoleEntitySafe(Role role) {
    try {
      return of(entityService.findByName(role.getName()).orElseGet(() -> entityService.create(role)));
    } catch (Exception e) {
      log.warn("Role entity creation failed for role name = {}", role.getName(), e);
      return empty();
    }
  }

  private static Roles buildRoles(List<Role> roles) {
    return new Roles().roles(roles).totalRecords((long) roles.size());
  }

  private static Roles buildRoles(PageResult<Role> roles) {
    return new Roles().roles(roles.getRecords()).totalRecords(roles.getTotalRecords());
  }

  private static void checkIfRoleHasDefaultType(Role role) {
    if (hasDefaultRoleType(role)) {
      log.debug("Default role cannot be created, updated or deleted via roles API: id = {}, name = {}", role.getId(),
        role.getName());
      throw new IllegalArgumentException("Default role cannot be created, updated or deleted via roles API.");
    }
  }

  private static boolean hasDefaultRoleType(Role role) {
    return RoleType.DEFAULT == role.getType();
  }
}
