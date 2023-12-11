package org.folio.roles.integration.keyclock.client;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.KeycloakRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloak-roles-users-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class)
public interface RolesUsersClient {

  @GetMapping("/admin/realms/{tenantId}/users/{userId}/role-mappings/realm")
  List<KeycloakRole> findRolesUsers(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                                        @PathVariable UUID userId);

  /**
   * Unlinks all request's roles from user. If role is not assigned to user, keycloak returns 204. If there
   * is no at least one of the request's roles in realm, keycloak returns 404 and doesn't unlink any role from request.
   *
   * @param tenantId tenant identifier
   * @param userId   keycloak user unique identifier
   */
  @DeleteMapping("/admin/realms/{tenantId}/users/{userId}/role-mappings/realm")
  void unlinkRolesFromUser(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                           @PathVariable UUID userId, @RequestBody List<KeycloakRole> request);

  /**
   * Assign all request's roles from user. If role is not assigned to user, keycloak returns 204. If there
   * is no at least one of the request's roles in realm, keycloak returns 404 and doesn't assign any role from request.
   *
   * @param tenantId tenant identifier
   * @param userId   keycloak user unique identifier
   */
  @PostMapping("/admin/realms/{tenantId}/users/{userId}/role-mappings/realm")
  void assignRolesToUser(@RequestHeader(AUTHORIZATION) String token, @PathVariable String tenantId,
                         @PathVariable UUID userId, @RequestBody List<KeycloakRole> request);
}
