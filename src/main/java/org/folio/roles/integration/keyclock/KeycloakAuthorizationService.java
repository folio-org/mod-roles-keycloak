package org.folio.roles.integration.keyclock;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.utils.JsonHelper;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakAuthorizationService {

  private final JsonHelper jsonHelper;
  private final KeycloakAuthorizationClientProvider authResourceProvider;

  /**
   * Creates keycloak permissions based on provided policy and list of endpoints.
   *
   * @param policy - {@link Policy} to be used to assign keycloak permission to resource/scope
   * @param endpoints - list with resource-scope identifiers
   * @param nameGenerator - keycloak permission name generator
   */
  public void createPermissions(Policy policy, List<Endpoint> endpoints, Function<Endpoint, String> nameGenerator) {
    if (policy == null || isEmpty(endpoints)) {
      log.debug("Keycloak permissions creation skipped [policy: {}, endpoints: {}]",
        () -> toJson(policy), () -> toJson(endpoints));
      return;
    }

    var endpointsByPath = endpoints.stream().collect(Collectors.groupingBy(Endpoint::getPath));

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = endpointsByPath.entrySet().stream()
        .map(entry -> CompletableFuture.runAsync(
          getRunnableWithCurrentFolioContext(
            () -> createPermissionsForPath(policy, entry.getKey(), entry.getValue(), nameGenerator)),
          executor))
        .toList();
      joinAll(futures);
    }
  }

  private void createPermissionsForPath(Policy policy, String path, List<Endpoint> endpointsForPath,
    Function<Endpoint, String> nameGenerator) {
    // KC proxy objects are RESTEasy stateless HTTP proxies: safe to call concurrently.
    // FolioExecutionContext is propagated to each virtual thread via getRunnableWithCurrentFolioContext().
    var scopePermissionsClient = getAuthorizationClient().permissions().scope();
    var resource = getAuthResourceByStaticPath(path);
    for (var endpoint : endpointsForPath) {
      var scope = getScopeByMethod(resource, endpoint.getMethod());
      if (scope.isEmpty()) {
        log.warn(
          "Scope is not found, keycloak permission creation will be skipped: method(scope)={}, path(resource)={}",
          endpoint.getMethod(), endpoint.getPath());
        continue;
      }
      var policyName = nameGenerator.apply(endpoint);
      var permission = buildPermissionFor(policyName, resource.getId(), scope.get().getId(), policy.getId());

      try (var response = scopePermissionsClient.create(permission)) {
        processKeycloakResponse(permission, response);
      }
    }
  }

  /**
   * Deletes keycloak permissions based on provided policy and list of endpoints.
   *
   * <p>If Keycloak permission is not found, implementation ignores this case and proceeds with the next permission</p>
   *
   * @param policy - {@link Policy} to be used to assign keycloak permission to resource/scope
   * @param endpoints - list with resource-scope identifiers
   * @param nameGenerator - keycloak permission name generator
   */
  public void deletePermissions(Policy policy, List<Endpoint> endpoints, Function<Endpoint, String> nameGenerator) {
    if (policy == null || isEmpty(endpoints)) {
      log.debug("Keycloak permissions deletion skipped [policy: {}, endpoints: {}]",
        () -> toJson(policy), () -> toJson(endpoints));
      return;
    }

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = endpoints.stream()
        .map(endpoint -> CompletableFuture.runAsync(
          // KC proxy objects are RESTEasy stateless HTTP proxies: safe to call concurrently.
          // FolioExecutionContext is propagated to each virtual thread via getRunnableWithCurrentFolioContext().
          getRunnableWithCurrentFolioContext(
            () -> removeKeycloakPermission(getAuthorizationClient().permissions().scope(), endpoint, nameGenerator)),
          executor))
        .toList();
      joinAll(futures);
    }
  }

  private ResourceRepresentation getAuthResourceByStaticPath(String staticPath) {
    log.debug("Searching for Keycloak resource by permission: {}", staticPath);
    var resources = getAuthorizationClient().resources().find(staticPath, null, null, null, null, 0, MAX_VALUE);

    var resourceRepresentation = resources.stream()
      .filter(resource -> StringUtils.equals(staticPath, resource.getName()))
      .findFirst()
      .orElseThrow(() -> new EntityNotFoundException("Keycloak resource is not found by static path: " + staticPath));

    log.debug("Keycloak resource found [value: {}]", () -> jsonHelper.asJsonStringSafe(resourceRepresentation));
    return resourceRepresentation;
  }

  private static Optional<ScopeRepresentation> getScopeByMethod(ResourceRepresentation resource, HttpMethod method) {
    return resource.getScopes().stream()
      .filter(scope -> equalsIgnoreCase(scope.getName(), method.toString()))
      .findFirst();
  }

  private AuthorizationResource getAuthorizationClient() {
    return authResourceProvider.getAuthorizationClient();
  }

  private String toJson(Object value) {
    return jsonHelper.asJsonString(value);
  }

  private static ScopePermissionRepresentation buildPermissionFor(
    String name, String resId, String scopeId, UUID policyId) {
    var permission = new ScopePermissionRepresentation();

    permission.setName(name);
    permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
    permission.setPolicies(Set.of(policyId.toString()));
    permission.setResources(Set.of(resId));
    permission.setScopes(Set.of(scopeId));

    return permission;
  }

  private static void removeKeycloakPermission(ScopePermissionsResource client,
    Endpoint endpoint, Function<Endpoint, String> nameGenerator) {
    var permissionName = nameGenerator.apply(endpoint);
    var foundPermission = client.findByName(permissionName);
    if (foundPermission == null) {
      log.info("Keycloak permission is not found [name: {}]", permissionName);
      return;
    }

    client.findById(foundPermission.getId()).remove();
    log.debug("Permission removed from Keycloak [name: {}]", permissionName);
  }

  private static void processKeycloakResponse(ScopePermissionRepresentation permission, Response response) {
    var statusInfo = response.getStatusInfo();
    if (statusInfo.getFamily() == SUCCESSFUL) {
      log.debug("Permission created in Keycloak [name: {}]", permission.getName());
      return;
    }

    if (statusInfo.toEnum() == CONFLICT) {
      log.debug("Permission already exists in Keycloak [name: {}]", permission.getName());
      return;
    }

    throw new ServiceException(format(
      "Error during scope-based permission creation in Keycloak. Details: status = %s, message = %s",
      statusInfo.getStatusCode(), statusInfo.getReasonPhrase()),
      "permission", permission.getName());
  }

  private static void joinAll(List<CompletableFuture<Void>> futures) {
    try {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    } catch (CompletionException ex) {
      var cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw ex;
    }
  }
}
