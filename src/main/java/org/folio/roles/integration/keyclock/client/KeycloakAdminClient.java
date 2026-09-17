package org.folio.roles.integration.keyclock.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * Spring HTTP-interface client for the Keycloak Admin REST API, replacing the RESTEasy
 * {@code keycloak-admin-client} fluent proxy. It reuses the Keycloak representation DTOs shipped in
 * {@code keycloak-client-common-synced}; the bearer token is injected by a request interceptor wired in
 * {@link org.folio.roles.integration.keyclock.configuration.KeycloakConfiguration}.
 *
 * <p>Realm-scoped operations take a {@code realm} path variable; authorization-services operations additionally
 * take the login client's UUID ({@code clientUuid}). Endpoint paths mirror the Keycloak Admin REST API exactly
 * so behaviour is identical to the former admin-client calls.</p>
 */
@HttpExchange(accept = APPLICATION_JSON_VALUE)
public interface KeycloakAdminClient {

  // ---------------------------------------------------------------------------------------------------------
  // Realm roles
  // ---------------------------------------------------------------------------------------------------------

  /** GET /admin/realms/{realm}/roles/{roleName} — fetch a realm role by name (404 if absent). */
  @GetExchange("/admin/realms/{realm}/roles/{roleName}")
  RoleRepresentation getRoleByName(@PathVariable String realm, @PathVariable String roleName);

  /** POST /admin/realms/{realm}/roles — create a realm role. */
  @PostExchange(value = "/admin/realms/{realm}/roles", contentType = APPLICATION_JSON_VALUE)
  void createRole(@PathVariable String realm, @RequestBody RoleRepresentation role);

  /** PUT /admin/realms/{realm}/roles-by-id/{id} — update a realm role by its identifier. */
  @PutExchange(value = "/admin/realms/{realm}/roles-by-id/{id}", contentType = APPLICATION_JSON_VALUE)
  void updateRoleById(@PathVariable String realm, @PathVariable String id, @RequestBody RoleRepresentation role);

  /** DELETE /admin/realms/{realm}/roles-by-id/{id} — delete a realm role by its identifier. */
  @DeleteExchange("/admin/realms/{realm}/roles-by-id/{id}")
  void deleteRoleById(@PathVariable String realm, @PathVariable String id);

  // ---------------------------------------------------------------------------------------------------------
  // Clients
  // ---------------------------------------------------------------------------------------------------------

  /** GET /admin/realms/{realm}/clients?clientId={clientId} — find clients by client-id. */
  @GetExchange("/admin/realms/{realm}/clients")
  List<ClientRepresentation> findClientsByClientId(@PathVariable String realm,
    @RequestParam("clientId") String clientId);

  // ---------------------------------------------------------------------------------------------------------
  // Users
  // ---------------------------------------------------------------------------------------------------------

  /** GET /admin/realms/{realm}/users?briefRepresentation={brief}&q={query} — search users by attributes. */
  @GetExchange("/admin/realms/{realm}/users")
  List<UserRepresentation> searchUsersByAttributes(@PathVariable String realm,
    @RequestParam("briefRepresentation") boolean briefRepresentation, @RequestParam("q") String query);

  /** GET /admin/realms/{realm}/users?first={first}&max={max} — list users with paging. */
  @GetExchange("/admin/realms/{realm}/users")
  List<UserRepresentation> listUsers(@PathVariable String realm,
    @RequestParam("first") Integer first, @RequestParam("max") Integer max);

  // ---------------------------------------------------------------------------------------------------------
  // Realm-level role mappings
  // ---------------------------------------------------------------------------------------------------------

  /** POST /admin/realms/{realm}/users/{userId}/role-mappings/realm — assign realm roles to a user. */
  @PostExchange(value = "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
    contentType = APPLICATION_JSON_VALUE)
  void addRealmRoleMappings(@PathVariable String realm, @PathVariable String userId,
    @RequestBody List<RoleRepresentation> roles);

  /** DELETE /admin/realms/{realm}/users/{userId}/role-mappings/realm — remove realm roles from a user. */
  @DeleteExchange(value = "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
    contentType = APPLICATION_JSON_VALUE)
  void removeRealmRoleMappings(@PathVariable String realm, @PathVariable String userId,
    @RequestBody List<RoleRepresentation> roles);

  // ---------------------------------------------------------------------------------------------------------
  // Authorization services — resources (client-scoped: /clients/{clientUuid}/authz/resource-server)
  // ---------------------------------------------------------------------------------------------------------

  /** GET .../authz/resource-server/resource?name={name}&first={first}&max={max} — find protected resources. */
  @GetExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/resource")
  List<ResourceRepresentation> findAuthResources(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestParam("name") String name, @RequestParam("first") Integer first, @RequestParam("max") Integer max);

  // ---------------------------------------------------------------------------------------------------------
  // Authorization services — scope-based permissions
  // ---------------------------------------------------------------------------------------------------------

  /** POST .../authz/resource-server/permission/scope — create a scope-based permission. */
  @PostExchange(value = "/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/permission/scope",
    contentType = APPLICATION_JSON_VALUE)
  void createScopePermission(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestBody ScopePermissionRepresentation permission);

  /** GET .../authz/resource-server/permission/scope/search?name={name} — find a scope permission by name. */
  @GetExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/permission/scope/search")
  ScopePermissionRepresentation findScopePermissionByName(@PathVariable String realm,
    @PathVariable String clientUuid, @RequestParam("name") String name);

  /** DELETE .../authz/resource-server/permission/scope/{permissionId} — delete a scope permission. */
  @DeleteExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/permission/scope/{permissionId}")
  void deleteScopePermissionById(@PathVariable String realm, @PathVariable String clientUuid,
    @PathVariable String permissionId);

  // ---------------------------------------------------------------------------------------------------------
  // Authorization services — policies
  // ---------------------------------------------------------------------------------------------------------

  /** GET .../authz/resource-server/policy?name={name}&permission={permission}&first={first}&max={max}. */
  @GetExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/policy")
  List<PolicyRepresentation> findPolicies(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestParam("name") String name, @RequestParam("permission") boolean permission,
    @RequestParam("first") Integer first, @RequestParam("max") Integer max);

  /** GET .../authz/resource-server/policy/{policyId} — fetch a policy by identifier (404 if absent). */
  @GetExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/policy/{policyId}")
  PolicyRepresentation getPolicyById(@PathVariable String realm, @PathVariable String clientUuid,
    @PathVariable String policyId);

  /** POST .../authz/resource-server/policy — create a policy (type carried in the representation). */
  @PostExchange(value = "/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/policy",
    contentType = APPLICATION_JSON_VALUE)
  void createPolicy(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestBody PolicyRepresentation policy);

  /** PUT .../authz/resource-server/policy/{policyId} — update a policy by identifier. */
  @PutExchange(value = "/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/policy/{policyId}",
    contentType = APPLICATION_JSON_VALUE)
  void updatePolicyById(@PathVariable String realm, @PathVariable String clientUuid, @PathVariable String policyId,
    @RequestBody PolicyRepresentation policy);

  /** DELETE .../authz/resource-server/policy/{policyId} — delete a policy by identifier. */
  @DeleteExchange("/admin/realms/{realm}/clients/{clientUuid}/authz/resource-server/policy/{policyId}")
  void deletePolicyById(@PathVariable String realm, @PathVariable String clientUuid, @PathVariable String policyId);
}
