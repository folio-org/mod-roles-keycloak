package org.folio.roles.integration.keyclock;

import static java.util.Optional.empty;
import static org.folio.common.utils.CollectionUtils.mapItems;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

  private final Keycloak keycloak;
  private final KeycloakRoleMapper keycloakRoleMapper;
  private final FolioExecutionContext context;

  /**
   * Tries to find role by identifier.
   *
   * @param id - role identifier
   * @return {@link Optional} with found {@link Role} object, {@link Optional#empty()} if entity is not found
   * @throws KeycloakApiException if role cannot be retrieved from Keycloak
   */
  public Optional<Role> findById(UUID id) {
    try {
      var realmResource = keycloak.realm(context.getTenantId());
      var keycloakRoleById = realmResource.rolesById().getRole(id.toString());
      log.debug("Role has been found by id: id = {}, name = {}", id, keycloakRoleById.getName());
      return Optional.of(keycloakRoleMapper.toRole(keycloakRoleById));
    } catch (NotFoundException nf) {
      log.debug("Role hasn't been found by id: id = {}", id);
      return Optional.empty();
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to find role: id = " + id, exception);
    }
  }

  public Optional<Role> findByName(String name) {
    var realmResource = keycloak.realm(context.getTenantId());
    try {
      var keycloakRoleByName = realmResource.roles().get(name).toRepresentation();
      log.debug("Role has been found by name: name = {}", name);
      return Optional.of(keycloakRoleMapper.toRole(keycloakRoleByName));
    } catch (NotFoundException notFoundException) {
      log.debug("Role hasn't been found by name: name = {}", name);
      return Optional.empty();
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to find role by name: " + name, exception);
    }
  }

  /**
   * Retrieves role by identifier.
   *
   * @param id - role identifier
   * @return found {@link Role} object
   * @throws EntityNotFoundException if role is not found by identifier
   * @throws KeycloakApiException if role cannot be retrieved from Keycloak
   */
  public Role getById(UUID id) {
    return findById(id).orElseThrow(() -> new EntityNotFoundException("Failed to find role: id = " + id));
  }

  /**
   * Searches role by query using pagination parameters.
   *
   * @param query - string query
   * @param offset - offset in pagination from first record
   * @param limit - a number of results in response
   * @return {@link PageResult} with found {@link Roles} object
   */
  public PageResult<Role> search(String query, Integer offset, Integer limit) {
    var realmResource = keycloak.realm(context.getTenantId());
    try {
      var keycloakRoles = realmResource.roles().list(query, offset, limit, false);
      var roles = mapItems(keycloakRoles, keycloakRoleMapper::toRole);
      log.debug("Roles have been found: names = {}", () -> mapItems(roles, Role::getName));
      return PageResult.of(roles.size(), roles);
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to search roles by query: " + query, exception);
    }
  }

  /**
   * Creates a role in Keycloak.
   *
   * @param role - role to create
   * @return created {@link Role} object
   */
  public Role create(Role role) {
    var realmResource = keycloak.realm(context.getTenantId());
    var keycloakRole = keycloakRoleMapper.toKeycloakRole(role);
    try {
      var rolesResource = realmResource.roles();
      rolesResource.create(keycloakRole);
      var foundKeycloakRole = rolesResource.get(keycloakRole.getName()).toRepresentation();
      log.debug("Role has been created: id = {}, name = {}", role.getId(), role.getName());
      return keycloakRoleMapper.toRole(foundKeycloakRole);
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to create keycloak role", exception);
    }
  }

  /**
   * Creates a role, suppressing all exception during create process.
   *
   * @param role - role object to be created
   * @return {@link Optional} with created {@link Role}, or {@link Optional#empty()} if exception occurred
   */
  public Optional<Role> createSafe(Role role) {
    try {
      return Optional.of(create(role));
    } catch (Exception e) {
      log.debug("Failed to create role: name = {}", role.getName(), e);
      return empty();
    }
  }

  /**
   * Updates role in Keycloak by identifier.
   *
   * @param role - role to update
   * @return updated {@link Role} object
   * @throws IllegalArgumentException if role or role id are null
   */
  public Role update(Role role) {
    Assert.notNull(role, "Role cannot be null");
    Assert.notNull(role.getId(), "Role id cannot be null");
    var realmResource = keycloak.realm(context.getTenantId());
    var keycloakRole = keycloakRoleMapper.toKeycloakRole(role);

    try {
      realmResource.rolesById().updateRole(role.getId().toString(), keycloakRole);
      log.debug("Role has been updated: name = {}", role.getName());
      return role;
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to update role: " + role.getId(), exception);
    }
  }

  /**
   * Deletes role by identifier.
   *
   * @param id - role identifier
   * @throws IllegalArgumentException if role identifier is null
   * @throws KeycloakApiException if failed to delete a role
   */
  public void deleteById(UUID id) {
    var realmResource = keycloak.realm(context.getTenantId());
    try {
      realmResource.rolesById().deleteRole(id != null ? id.toString() : null);
      log.debug("Role has been deleted: id = {}", id);
    } catch (WebApplicationException exception) {
      throw new KeycloakApiException("Failed to delete role: " + id, exception);
    }
  }

  /**
   * Deletes role by identifier, suppressing all exception during delete process.
   *
   * @param id - role identifier
   */
  public void deleteByIdSafe(UUID id) {
    try {
      deleteById(id);
    } catch (Exception exception) {
      log.debug("Failed to delete Role in Keycloak: id = {}", id, exception);
    }
  }
}
