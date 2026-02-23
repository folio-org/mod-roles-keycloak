package org.folio.roles.integration.keyclock;

import static java.lang.String.format;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Log4j2
@Service
@Retryable(
  predicate = KeycloakMethodRetryPredicate.class,
  maxRetriesString = "#{@keycloakConfigurationProperties.retry.maxAttempts}",
  delayString = "#{@keycloakConfigurationProperties.retry.backoff.delayMs}"
)
@RequiredArgsConstructor
public class KeycloakRoleService {

  private final Keycloak keycloak;
  private final KeycloakRoleMapper keycloakRoleMapper;
  private final FolioExecutionContext context;

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
      var errorMessage = format("Failed to find role by name: %s", name);
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception);
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
      var errorMessage = format("Failed to create keycloak role: name = %s", role.getName());
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception);
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
      var errorMessage = format("Failed to update role: %s", role.getId());
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception);
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
      var errorMessage = format("Failed to delete role: %s", id);
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception);
    }
  }
}
