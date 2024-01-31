package org.folio.roles.service.permission;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.folio.common.utils.CollectionUtils.toStream;

import jakarta.ws.rs.ClientErrorException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationClientProvider;
import org.folio.roles.integration.permissions.PermissionsClient;
import org.folio.roles.integration.permissions.UserPermissions;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.service.role.UserRoleService;
import org.folio.spring.FolioExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PermissionMigrationService {

  private final Keycloak keycloak;
  private final RoleService roleService;
  private final PolicyService policyService;
  private final UserRoleService userRoleService;
  private final PermissionsClient permissionsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final KeycloakAuthorizationClientProvider authorizationService;

  public void migratePermissions() {
    log.info("Starting permission migration...");

    var userPermissions = loadUserPermissions();
    var roleFolioPermissionMap = toMap(emptyIfNull(userPermissions.values()),
      PermissionMigrationService::getPermissionsHash);
    var keycloakRoles = createKeycloakRoles(roleFolioPermissionMap);
    assignUserRoles(userPermissions, keycloakRoles);
    var createdPolicies = createPolicies(keycloakRoles);

    createPermissionsForRoles(createdPolicies, roleFolioPermissionMap);

    log.info("Migration of permissions is finished");
  }

  private Map<String, List<String>> loadUserPermissions() {
    var tenantId = folioExecutionContext.getTenantId();
    var realm = keycloak.realm(tenantId);
    var usersClient = realm.users();

    log.info("Loading keycloak users...");
    var keycloakUsers = loadPaginatedData(usersClient::list);
    log.info("Keycloak users are loaded, size: {}", keycloakUsers.size());
    var okapiUserIds = getOkapiUserIds(keycloakUsers);

    log.info("Loading user permissions...");
    var userPermissions = okapiUserIds.stream()
      .collect(Collectors.toMap(identity(), this::getUserPermissions, (o1, o2) -> o2));
    log.info("User permissions are loaded for {} user(s)", userPermissions.size());
    return userPermissions;
  }

  private static List<String> getOkapiUserIds(List<UserRepresentation> keycloakUsers) {
    return keycloakUsers.stream()
      .filter(userRepresentation -> userRepresentation.getAttributes() != null)
      .filter(userRepresentation -> userRepresentation.getAttributes().containsKey("user_id"))
      .map(userRepresentation -> userRepresentation.getAttributes().get("user_id"))
      .filter(CollectionUtils::isNotEmpty)
      .map(userIdValues -> userIdValues.get(0))
      .collect(toList());
  }

  private List<Policy> createPolicies(Map<String, Role> keycloakRoles) {
    log.info("Creating policies for roles...");
    var rolePolicies = toStream(keycloakRoles.values())
      .map(role -> new Policy()
        .type(PolicyType.ROLE)
        .name("Policy for role: " + role.getName())
        .description("System generated service policy during migration")
        .rolePolicy(new RolePolicy().roles(List.of(new RolePolicyRole().id(role.getId())))))
      .collect(toList());

    var createdPolicies = policyService.create(rolePolicies).getPolicies();
    if (createdPolicies.size() != rolePolicies.size()) {
      createdPolicies = new ArrayList<>();
      for (var policy : rolePolicies) {
        createdPolicies.addAll(policyService.search("name==" + policy.getName(), 100, 0).getPolicies());
      }
    }

    log.info("{} policies for roles created", createdPolicies.size());
    return createdPolicies;
  }

  private Map<String, Role> createKeycloakRoles(Map<String, List<String>> rolePermissionMap) {

    log.info("Creating {} role(s) in keycloak...", rolePermissionMap.size());
    var roles = toStream(rolePermissionMap.entrySet()).map(PermissionMigrationService::createRole).collect(toList());
    var createdRoles = roleService.create(roles).getRoles();

    if (createdRoles.size() != roles.size()) {
      createdRoles = new ArrayList<>();
      for (var role : roles) {
        createdRoles.addAll(roleService.search("name==" + role.getName(), 0, 100).getRoles());
      }
    }

    log.info("{} role(s) created", createdRoles.size());
    return toMap(createdRoles, Role::getName);
  }

  private void assignUserRoles(Map<String, List<String>> userPermissions, Map<String, Role> keycloakRoles) {
    log.info("Assigning roles to users...");

    var counter = new AtomicInteger(0);
    for (var entry : emptyIfNull(userPermissions.entrySet())) {
      var userId = UUID.fromString(entry.getKey());
      var role = getPermissionsHash(entry.getValue());

      try {
        userRoleService.create(new UserRolesRequest().userId(userId).addRoleIdsItem(keycloakRoles.get(role).getId()));
        counter.incrementAndGet();
      } catch (Exception e) {
        log.debug("Failed to assign role to user [userId={}, role={}, cause={}]", userId, role, e.getMessage());
      }
    }

    log.info("{} roles assigned to users", counter.get());
  }

  @NotNull
  private static Role createRole(Entry<String, List<String>> permissionsEntry) {
    return new Role()
      .name(permissionsEntry.getKey())
      .description("System generated role during migration");
  }

  private void createPermissionsForRoles(List<Policy> policies, Map<String, List<String>> roleFolioPermissionMap) {
    log.info("Creating permissions for roles...");
    var tenantAuthorizationClient = authorizationService.getAuthorizationClient();
    var authorizationResources = tenantAuthorizationClient.resources();

    var keycloakResources = loadPaginatedData(
      (offset, limit) -> authorizationResources.find(null, null, null, null, null, offset, limit));

    var keycloakScopes = tenantAuthorizationClient.scopes().scopes();
    var scopesByName = toMap(keycloakScopes, ScopeRepresentation::getName);

    var permissions = keycloakResources.stream()
      .map(resource -> getKeycloakResourceByFolioPermission(resource, scopesByName))
      .flatMap(map -> map.entrySet().stream())
      .collect(groupingBy(Entry::getKey, mapping(Entry::getValue, toSet())));

    var folioPermissionUniqueRoleMap = roleFolioPermissionMap.entrySet().stream()
      .flatMap(entry -> entry.getValue().stream().map(e -> new SimpleEntry<>(e, entry.getKey())))
      .collect(groupingBy(Entry::getKey, mapping(Entry::getValue, toSet())));

    var policiesByRole = toMap(policies, policy -> policy.getName().substring("Policy for role: ".length()));

    var authClientPermissions = tenantAuthorizationClient.permissions();
    createKeycloakPermissions(permissions, folioPermissionUniqueRoleMap, policiesByRole, authClientPermissions);
  }

  private static void createKeycloakPermissions(Map<String, Set<AuthResourceHolder>> permissions,
                                                Map<String, Set<String>> folioPermissionUniqueRoleMap,
                                                Map<String, Policy> policiesByRole,
                                                PermissionsResource permissionsClient) {
    var counter = new AtomicInteger();
    for (var entry : folioPermissionUniqueRoleMap.entrySet()) {
      processEntry(entry.getKey(), entry.getValue(), permissions, policiesByRole, permissionsClient, counter);
    }

    log.info("{} permission(s) created", counter.get());
  }

  private static void processEntry(String folioPerm,
                                   Set<String> assignedRoles, Map<String, Set<AuthResourceHolder>> permissions,
                                   Map<String, Policy> policiesByRole,
                                   PermissionsResource permissionsClient, AtomicInteger counter) {
    var keycloakPermissionHolder = permissions.get(folioPerm);
    if (keycloakPermissionHolder == null) {
      log.debug("Ignoring permission without resources: {}", folioPerm);
      return;
    }

    for (var authResourceHolder : keycloakPermissionHolder) {
      processAuthResourceHolder(authResourceHolder, assignedRoles, policiesByRole, permissionsClient, counter);
    }
  }

  private static void processAuthResourceHolder(AuthResourceHolder authResourceHolder,
                                                Set<String> assignedRoles, Map<String, Policy> policiesByRole,
                                                PermissionsResource permissionsClient, AtomicInteger counter) {
    var scopes = toStream(authResourceHolder.scopes)
      .map(ScopeRepresentation::getName)
      .sorted().collect(toList());
    var resourceName = authResourceHolder.resourceRepresentation.getName();
    for (var assignedRole : assignedRoles) {
      createPermission(authResourceHolder, assignedRole, scopes, resourceName, policiesByRole,
        permissionsClient, counter);
    }
  }

  private static void createPermission(AuthResourceHolder authResourceHolder, String assignedRole, List<String> scopes,
                                       String resourceName, Map<String, Policy> policiesByRole,
                                       PermissionsResource permissionsClient,
                                       AtomicInteger counter) {
    var permissionName = String.format("%s access for role '%s' to '%s'", scopes, assignedRole, resourceName);

    var permission = new ScopePermissionRepresentation();
    permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
    permission.setPolicies(Set.of(policiesByRole.get(assignedRole).getId().toString()));
    permission.setResources(Set.of(authResourceHolder.resourceRepresentation.getId()));
    permission.setScopes(getScopeIds(authResourceHolder).collect(toSet()));
    permission.setName(permissionName);

    try (var ignored = permissionsClient.scope().create(permission)) {
      counter.incrementAndGet();
      if (log.isDebugEnabled()) {
        log.debug("Permission was created with name: {}", permission.getName());
      }
    } catch (ClientErrorException exception) {
      if (exception.getResponse().getStatus() != SC_CONFLICT) {
        throw new MigrationException("Error during creating resources in Keycloak", exception);
      }
    }
  }

  private static Stream<String> getScopeIds(AuthResourceHolder authResourceHolder) {
    return authResourceHolder.scopes.stream().map(ScopeRepresentation::getId);
  }

  private static Map<String, AuthResourceHolder> getKeycloakResourceByFolioPermission(
    ResourceRepresentation res, Map<String, ScopeRepresentation> scopeRepresentationMap) {
    var attributes = res.getAttributes();
    var folioPermissions = attributes.get("folio_permissions");

    var resourceName = res.getName();
    if (CollectionUtils.isEmpty(folioPermissions)) {
      log.info("Skipping resource because folio permission is empty, name: {}", resourceName);
      return Collections.emptyMap();
    }

    var resultMap = new HashMap<String, AuthResourceHolder>();
    for (var permissionValue : folioPermissions) {
      var permissionWithScopes = permissionValue.split("#");
      if (permissionWithScopes.length == 1) {
        log.info("Skipping resource because folio permission does not have scopes, name: {}", resourceName);
        continue;
      }

      var scopes = new HashSet<ScopeRepresentation>();
      var folioPermissionName = permissionWithScopes[permissionWithScopes.length - 1];
      for (var j = 0; j < permissionWithScopes.length - 1; j++) {
        var scopeName = permissionWithScopes[j];
        var scopeRepresentation = scopeRepresentationMap.get(scopeName);
        if (scopeRepresentation != null) {
          scopes.add(scopeRepresentation);
        } else {
          log.info("Skipping scope because it is not defined in map [scope={}, resource={}]", scopeName, resourceName);
        }
      }

      if (!scopes.isEmpty()) {
        resultMap.put(folioPermissionName, AuthResourceHolder.of(scopes, res));
      }
    }

    return resultMap;
  }

  private static <T> List<T> loadPaginatedData(PaginationResourceSupplier<T> resourceSupplier) {
    int currBatchSize;
    var offset = 0;
    var limit = 100;
    var allUsers = new ArrayList<T>();

    do {
      var list = resourceSupplier.load(offset, limit);
      currBatchSize = list.size();
      offset += currBatchSize;
      allUsers.addAll(list);
    } while (currBatchSize > 0);

    return allUsers;
  }

  private List<String> getUserPermissions(String userId) {
    var userPermissions = permissionsClient.getUserPermissions(userId, "userId", true);

    return userPermissions.map(PermissionMigrationService::extractPermissionNames)
      .orElse(emptyList());
  }

  private static List<String> extractPermissionNames(UserPermissions userPermissions) {
    return toStream(userPermissions.getPermissionNames())
      .distinct().sorted()
      .collect(toList());
  }

  private static <K, V> Map<K, V> toMap(Collection<V> collection, Function<V, K> keyMapper) {
    return collection.stream().collect(Collectors.toMap(keyMapper, identity(), (o1, o2) -> o2));
  }

  private static String getPermissionsHash(List<String> permissions) {
    return sha1Hex(String.join("|", permissions)); //NOSONAR not used in secure contexts
  }

  interface PaginationResourceSupplier<T> {

    List<T> load(int offset, int limit);
  }

  @Data
  @RequiredArgsConstructor(staticName = "of")
  private static final class AuthResourceHolder {

    private final Set<ScopeRepresentation> scopes;
    private final ResourceRepresentation resourceRepresentation;
  }
}
