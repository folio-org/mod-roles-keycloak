package org.folio.roles.integration.keyclock;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Strings;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.utils.JsonHelper;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Log4j2
@Service
@Retryable(
  predicate = KeycloakMethodRetryPredicate.class,
  maxRetriesString = "#{@keycloakConfigurationProperties.retry.maxAttempts}",
  delayString = "#{@keycloakConfigurationProperties.retry.backoff.delayMs}"
)
@RequiredArgsConstructor
public class KeycloakAuthorizationService {

  private final JsonHelper jsonHelper;
  private final KeycloakAdminClient keycloakAdminClient;
  private final KeycloakClientService keycloakClientService;
  private final FolioExecutionContext context;
  private final KeycloakPermissionsExecutor permissionsExecutor;

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

    permissionsExecutor.execute(endpoints, endpoint -> createPermission(policy, endpoint, nameGenerator));
  }

  private void createPermission(Policy policy, Endpoint endpoint, Function<Endpoint, String> nameGenerator) {
    var realm = context.getTenantId();
    var clientUuid = getLoginClientUuid();
    var resource = getAuthResourceByStaticPath(realm, clientUuid, endpoint.getPath());
    var scope = getScopeByMethod(resource, endpoint.getMethod());
    if (scope.isEmpty()) {
      log.warn(
        "Scope is not found, keycloak permission creation will be skipped: method(scope)={}, path(resource)={}",
        endpoint.getMethod(), endpoint.getPath());
      return;
    }
    var policyName = nameGenerator.apply(endpoint);
    var permission = buildPermissionFor(policyName, resource.getId(), scope.get().getId(), policy.getId());
    try {
      keycloakAdminClient.createScopePermission(realm, clientUuid, permission);
      log.debug("Permission created in Keycloak [name: {}]", permission.getName());
    } catch (RestClientResponseException exception) {
      processKeycloakPermissionFailure(permission, exception);
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

    permissionsExecutor.execute(endpoints, endpoint -> removeKeycloakPermission(endpoint, nameGenerator));
  }

  private ResourceRepresentation getAuthResourceByStaticPath(String realm, String clientUuid, String staticPath) {
    log.debug("Searching for Keycloak resource by permission: {}", staticPath);
    var resources = keycloakAdminClient.findAuthResources(realm, clientUuid, staticPath, 0, MAX_VALUE);

    var resourceRepresentation = resources.stream()
      .filter(resource -> Strings.CS.equals(staticPath, resource.getName()))
      .findFirst()
      .orElseThrow(() -> new EntityNotFoundException("Keycloak resource is not found by static path: " + staticPath));

    log.debug("Keycloak resource found [value: {}]", () -> jsonHelper.asJsonStringSafe(resourceRepresentation));
    return resourceRepresentation;
  }

  private static Optional<ScopeRepresentation> getScopeByMethod(ResourceRepresentation resource, HttpMethod method) {
    return resource.getScopes().stream()
      .filter(scope -> Strings.CI.equals(scope.getName(), method.toString()))
      .findFirst();
  }

  private String getLoginClientUuid() {
    return keycloakClientService.getLoginClient().getId();
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

  private void removeKeycloakPermission(Endpoint endpoint, Function<Endpoint, String> nameGenerator) {
    var realm = context.getTenantId();
    var clientUuid = getLoginClientUuid();
    var permissionName = nameGenerator.apply(endpoint);
    var foundPermission = findScopePermissionByName(realm, clientUuid, permissionName);
    if (foundPermission == null) {
      log.info("Keycloak permission is not found [name: {}]", permissionName);
      return;
    }

    keycloakAdminClient.deleteScopePermissionById(realm, clientUuid, foundPermission.getId());
    log.debug("Permission removed from Keycloak [name: {}]", permissionName);
  }

  private ScopePermissionRepresentation findScopePermissionByName(String realm, String clientUuid, String name) {
    try {
      return keycloakAdminClient.findScopePermissionByName(realm, clientUuid, name);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == NOT_FOUND.value()) {
        return null;
      }
      throw exception;
    }
  }

  private static void processKeycloakPermissionFailure(ScopePermissionRepresentation permission,
    RestClientResponseException exception) {
    if (exception.getStatusCode().value() == CONFLICT.value()) {
      log.info("Permission already exists in Keycloak [name: {}]", permission.getName());
      return;
    }

    throw new ServiceException(format(
      "Error during scope-based permission creation in Keycloak. Details: status = %s, message = %s",
      exception.getStatusCode().value(), exception.getStatusText()),
      "permission", permission.getName());
  }
}
