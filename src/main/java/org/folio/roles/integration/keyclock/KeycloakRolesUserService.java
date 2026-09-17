package org.folio.roles.integration.keyclock;

import static org.folio.common.utils.CollectionUtils.mapItems;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakRolesUserService {

  private final KeycloakAdminClient keycloakAdminClient;
  private final FolioExecutionContext context;
  private final KeycloakUserService userService;
  private final KeycloakRoleMapper keycloakRoleMapper;

  public void assignRolesToUser(UUID userId, List<Role> roles) {
    var keycloakRoles = mapItems(roles, keycloakRoleMapper::toKeycloakRole);
    var keycloakUserId = userService.findKeycloakIdByUserId(userId);

    try {
      keycloakAdminClient.addRealmRoleMappings(context.getTenantId(), keycloakUserId, keycloakRoles);
      log.debug("Roles user have been assigned: userId = {}, keycloakUserId = {}", userId, keycloakUserId);
    } catch (RestClientResponseException exception) {
      throw new KeycloakApiException("Failed to assign roles to user: userId = " + userId, exception,
        exception.getStatusCode().value());
    }
  }

  public void unlinkRolesFromUser(UUID userId, List<Role> roles) {
    var keycloakRoles = mapItems(roles, keycloakRoleMapper::toKeycloakRole);
    var keycloakUserId = userService.findKeycloakIdByUserId(userId);

    try {
      keycloakAdminClient.removeRealmRoleMappings(context.getTenantId(), keycloakUserId, keycloakRoles);
      log.debug("Roles user have been unlinked: userId = {}, keycloakUserId = {}", userId, keycloakUserId);
    } catch (RestClientResponseException exception) {
      throw new KeycloakApiException("Failed to unlink roles from user: userId = " + userId, exception,
        exception.getStatusCode().value());
    }
  }
}
