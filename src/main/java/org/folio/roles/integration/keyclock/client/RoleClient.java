package org.folio.roles.integration.keyclock.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.KeycloakRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A Keycloak feign client for operations with roles.
 */
@FeignClient(
  name = "keycloak-roles-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class
)
public interface RoleClient {

  /**
   * Find one or more roles.
   *
   * @param token  authorization header value.
   * @param first  number of the first element in response.
   * @param max    element's count in response.
   * @param search query for searching elements.
   * @return list of {@link KeycloakRole}.
   */
  @GetMapping(value = "/admin/realms/{realm}/roles")
  List<KeycloakRole> find(@PathVariable String realm,
                          @RequestHeader(AUTHORIZATION) String token,
                          @RequestParam(required = false) Integer first,
                          @RequestParam(required = false) Integer max,
                          @RequestParam(required = false) String search);

  /**
   * Find single role by identifier.
   *
   * @param token  authorization header value.
   * @param roleId role identifier.
   * @return single {@link KeycloakRole}.
   */
  @GetMapping("/admin/realms/{realm}/roles-by-id/{roleId}")
  KeycloakRole findById(@PathVariable String realm,
                        @RequestHeader(AUTHORIZATION) String token,
                        @PathVariable UUID roleId);

  /**
   * Find single role by name.
   *
   * @param token    authorization header value.
   * @param roleName name of searching role.
   * @return single {@link KeycloakRole}.
   */
  @GetMapping("/admin/realms/{realm}/roles/{roleName}")
  KeycloakRole findByName(@PathVariable String realm,
                          @RequestHeader(AUTHORIZATION) String token,
                          @PathVariable String roleName);

  /**
   * Create one role.
   *
   * @param token   authorization header value.
   * @param request {@link KeycloakRole} role for creating.
   */
  @PostMapping("/admin/realms/{realm}/roles/")
  void create(@PathVariable String realm,
              @RequestHeader(AUTHORIZATION) String token,
              @RequestBody KeycloakRole request);

  /**
   * Update single role by id.
   *
   * @param token   authorization header value.
   * @param roleId  role identifier.
   * @param request {@link KeycloakRole} role for updating.
   */
  @PutMapping("/admin/realms/{realm}/roles-by-id/{roleId}")
  void updateById(@PathVariable String realm,
                  @RequestHeader(AUTHORIZATION) String token,
                  @PathVariable UUID roleId,
                  @RequestBody KeycloakRole request);

  /**
   * Delete role by id.
   *
   * @param token  authorization header value.
   * @param roleId role identifier.
   */
  @DeleteMapping("/admin/realms/{realm}/roles-by-id/{roleId}")
  void deleteById(@PathVariable String realm,
                  @RequestHeader(AUTHORIZATION) String token,
                  @PathVariable UUID roleId);
}
