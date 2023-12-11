package org.folio.roles.integration.keyclock;

import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.client.RolesUsersClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRolesUserMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakRolesUserService {

  private final RolesUsersClient rolesUserClient;
  private final FolioExecutionContext context;
  private final KeycloakUserService userService;
  private final KeycloakRolesUserMapper mapper;
  private final KeycloakAccessTokenService tokenService;

  public void assignRolesToUser(UUID userId, List<Role> roles) {
    var request = mapper.toKeycloakDto(roles);
    var keycloakUserId = userService.findKeycloakIdByUserId(userId);
    try {
      rolesUserClient.assignRolesToUser(tokenService.getToken(), context.getTenantId(), keycloakUserId, request);
      log.debug("Roles user have been assigned: userId = {}, keycloakUserId = {}", userId, keycloakUserId);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to assign roles to user: userId = " + userId, e, e.status());
    }
  }

  public void unlinkRolesFromUser(UUID userId, List<Role> roles) {
    var request = mapper.toKeycloakDto(roles);
    var keycloakUserId = userService.findKeycloakIdByUserId(userId);
    try {
      rolesUserClient.unlinkRolesFromUser(tokenService.getToken(), context.getTenantId(), keycloakUserId,
        request);
      log.debug("Roles user have been unlinked: userId = {}, keycloakUserId = {}", userId, keycloakUserId);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to unlink roles from user: userId = " + userId, e, e.status());
    }
  }
}
