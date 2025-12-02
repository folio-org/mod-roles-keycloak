package org.folio.roles.service.role;

import static java.util.Optional.empty;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
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
   * @return {@link RoleCreationResult} with successful roles and failure details
   */
  @Transactional
  public RoleCreationResult createRolesSafely(List<Role> roles) {
    var result = new RoleCreationResult();
    
    for (Role role : roles) {
      createRoleSafely(role, result);
    }
    
    return result;
  }

  /**
   * Creates a single role with safe error handling.
   * Adds result to the provided RoleCreationResult object.
   *
   * @param role - role to create
   * @param result - result object to populate
   */
  private void createRoleSafely(Role role, RoleCreationResult result) {
    var keycloakRoleOpt = createInKeycloak(role, result);
    if (keycloakRoleOpt.isEmpty()) {
      handleKeycloakCreationFailure(role, result);
      return;
    }

    var keycloakRole = keycloakRoleOpt.get();
    keycloakRole.setType(role.getType());
    createInDatabase(keycloakRole, result);
  }

  /**
   * Creates role in Keycloak, capturing exceptions.
   *
   * @param role - role to create
   * @param result - result object to record failures
   * @return {@link Optional} with created role or empty if failed
   */
  private Optional<Role> createInKeycloak(Role role, RoleCreationResult result) {
    try {
      return Optional.of(keycloakRoleService.create(role));
    } catch (Exception e) {
      log.debug("Failed to create role in Keycloak: name = {}", role.getName(), e);
      result.addFailure(role.getName(), "Failed to create role in Keycloak", e);
      return empty();
    }
  }

  /**
   * Handles the case when Keycloak role creation fails.
   * Checks if role already exists and creates DB entity if needed.
   *
   * @param role - role that failed to create
   * @param result - result object to record success/failures
   */
  private void handleKeycloakCreationFailure(Role role, RoleCreationResult result) {
    if (roleEntityService.findByName(role.getName()).isEmpty()) {
      keycloakRoleService.findByName(role.getName()).ifPresent(existingRole -> {
        existingRole.setType(role.getType());
        createInDatabase(existingRole, result);
      });
    }
  }

  /**
   * Creates role entity in database with rollback on failure.
   *
   * @param role - role to create (must have ID from Keycloak)
   * @param result - result object to record success/failures
   */
  private void createInDatabase(Role role, RoleCreationResult result) {
    try {
      var existingRole = roleEntityService.findByName(role.getName());
      if (existingRole.isPresent()) {
        result.addSuccess(existingRole.get());
        return;
      }

      var createdRole = roleEntityService.create(role);
      result.addSuccess(createdRole);
    } catch (Exception e) {
      log.warn("Database role creation failed: name = {}, rolling back Keycloak role", role.getName(), e);
      result.addFailure(role.getName(), "Failed to create role in database", e);
      rollbackKeycloakRole(role);
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
}
