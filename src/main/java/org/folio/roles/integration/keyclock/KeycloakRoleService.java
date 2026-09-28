package org.folio.roles.integration.keyclock;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientResponseException;

@Log4j2
@Service
@Retryable(
  predicate = KeycloakMethodRetryPredicate.class,
  maxRetriesString = "#{@keycloakConfigurationProperties.retry.maxAttempts}",
  delayString = "#{@keycloakConfigurationProperties.retry.backoff.delayMs}"
)
@RequiredArgsConstructor
public class KeycloakRoleService {

  private final KeycloakAdminClient keycloakAdminClient;
  private final KeycloakRoleMapper keycloakRoleMapper;
  private final FolioExecutionContext context;

  public Optional<Role> findByName(String name) {
    try {
      var keycloakRoleByName = keycloakAdminClient.getRoleByName(context.getTenantId(), name);
      log.debug("Role has been found by name: name = {}", name);
      return Optional.of(keycloakRoleMapper.toRole(keycloakRoleByName));
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == NOT_FOUND.value()) {
        log.debug("Role hasn't been found by name: name = {}", name);
        return Optional.empty();
      }
      var errorMessage = format("Failed to find role by name: %s", name);
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception, exception.getStatusCode().value());
    }
  }

  /**
   * Creates a role in Keycloak.
   *
   * @param role - role to create
   * @return created {@link Role} object
   */
  public Role create(Role role) {
    var realm = context.getTenantId();
    var keycloakRole = keycloakRoleMapper.toKeycloakRole(role);
    try {
      keycloakAdminClient.createRole(realm, keycloakRole);
      var foundKeycloakRole = keycloakAdminClient.getRoleByName(realm, keycloakRole.getName());
      log.debug("Role has been created: id = {}, name = {}", role.getId(), role.getName());
      return keycloakRoleMapper.toRole(foundKeycloakRole);
    } catch (RestClientResponseException exception) {
      var errorMessage = format("Failed to create keycloak role: name = %s", role.getName());
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception, exception.getStatusCode().value());
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
    var keycloakRole = keycloakRoleMapper.toKeycloakRole(role);

    try {
      keycloakAdminClient.updateRoleById(context.getTenantId(), role.getId().toString(), keycloakRole);
      log.debug("Role has been updated: name = {}", role.getName());
      return role;
    } catch (RestClientResponseException exception) {
      var errorMessage = format("Failed to update role: %s", role.getId());
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception, exception.getStatusCode().value());
    }
  }

  /**
   * Deletes role by identifier.
   *
   * @param id - role identifier
   * @throws KeycloakApiException if failed to delete a role
   */
  public void deleteById(UUID id) {
    try {
      keycloakAdminClient.deleteRoleById(context.getTenantId(), id != null ? id.toString() : null);
      log.debug("Role has been deleted: id = {}", id);
    } catch (RestClientResponseException exception) {
      var errorMessage = format("Failed to delete role: %s", id);
      log.debug(errorMessage);
      throw new KeycloakApiException(errorMessage, exception, exception.getStatusCode().value());
    }
  }
}
