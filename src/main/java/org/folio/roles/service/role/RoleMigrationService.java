package org.folio.roles.service.role;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for role operations during migration process.
 * Handles role creation with graceful error handling and automatic rollback.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RoleMigrationService {

  private final KeycloakRoleService keycloakRoleService;
  private final RoleEntityService roleEntityService;

  /**
   * Creates multiple roles with safe error handling for migration purposes.
   * Skips roles that fail to create without throwing exceptions.
   *
   * @param roles - list of roles to create
   * @return {@link Roles} containing successfully created roles
   */
  @Transactional
  public Roles createRolesSafely(List<Role> roles) {
    var createdRoles = roles.stream()
      .map(this::createRoleSafely)
      .flatMap(Optional::stream)
      .toList();
    return buildRolesResponse(createdRoles);
  }

  /**
   * Creates a single role with safe error handling.
   * Returns empty if role creation fails or role already exists.
   *
   * @param role - role to create
   * @return {@link Optional} with created role or empty if failed
   */
  private Optional<Role> createRoleSafely(Role role) {
    var keycloakRoleOpt = createInKeycloak(role);
    if (keycloakRoleOpt.isEmpty()) {
      return handleKeycloakCreationFailure(role);
    }

    var keycloakRole = keycloakRoleOpt.get();
    keycloakRole.setType(role.getType());
    return createInDatabase(keycloakRole);
  }

  /**
   * Creates role in Keycloak, suppressing exceptions.
   *
   * @param role - role to create
   * @return {@link Optional} with created role or empty if failed
   */
  private Optional<Role> createInKeycloak(Role role) {
    try {
      return Optional.of(keycloakRoleService.create(role));
    } catch (Exception e) {
      log.debug("Failed to create role in Keycloak: name = {}", role.getName(), e);
      return empty();
    }
  }

  /**
   * Handles the case when Keycloak role creation fails.
   * Checks if role already exists and creates DB entity if needed.
   *
   * @param role - role that failed to create
   * @return {@link Optional} empty (role not created in this flow)
   */
  private Optional<Role> handleKeycloakCreationFailure(Role role) {
    if (roleEntityService.findByName(role.getName()).isEmpty()) {
      keycloakRoleService.findByName(role.getName()).ifPresent(existingRole -> {
        existingRole.setType(role.getType());
        createInDatabase(existingRole);
      });
    }
    return empty();
  }

  /**
   * Creates role entity in database with rollback on failure.
   *
   * @param role - role to create (must have ID from Keycloak)
   * @return {@link Optional} with created role or empty if failed
   */
  private Optional<Role> createInDatabase(Role role) {
    try {
      var existingRole = roleEntityService.findByName(role.getName());
      if (existingRole.isPresent()) {
        return existingRole;
      }

      var createdRole = roleEntityService.create(role);
      return of(createdRole);
    } catch (Exception e) {
      log.warn("Database role creation failed: name = {}, rolling back Keycloak role", role.getName(), e);
      rollbackKeycloakRole(role);
      return empty();
    }
  }

  /**
   * Rolls back Keycloak role by deleting it.
   *
   * @param role - role to rollback
   */
  private void rollbackKeycloakRole(Role role) {
    if (role.getId() == null) {
      return;
    }

    try {
      keycloakRoleService.deleteById(role.getId());
      log.info("Keycloak role rolled back successfully: id = {}, name = {}", role.getId(), role.getName());
    } catch (Exception rollbackException) {
      log.error("Keycloak role rollback failed: id = {}, name = {}", 
        role.getId(), role.getName(), rollbackException);
    }
  }

  /**
   * Builds Roles response from list of Role objects.
   *
   * @param roles - list of roles
   * @return {@link Roles} response object
   */
  private static Roles buildRolesResponse(List<Role> roles) {
    return new Roles().roles(roles).totalRecords((long) roles.size());
  }
}
