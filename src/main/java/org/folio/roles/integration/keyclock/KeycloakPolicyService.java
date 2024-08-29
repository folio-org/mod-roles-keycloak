package org.folio.roles.integration.keyclock;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.roles.mapper.KeycloakPolicyMapper.PolicyMapperContext;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.springframework.stereotype.Service;

/**
 * Keycloak policy service for operations with Keycloak policies.
 */
@Log4j2
@Service
@AllArgsConstructor
public class KeycloakPolicyService {

  private static final String FAILED_TO_FIND_POLICY_ERROR_TEMPLATE = "Failed to find policy: id = ";

  private final KeycloakUserService userService;
  private final KeycloakPolicyMapper keycloakPolicyMapper;
  private final KeycloakAuthorizationClientProvider authResourceClientProvider;

  /**
   * Searches keycloak policies by query and paging parameters - limit and offset.
   *
   * @param query - string query
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record
   * @return {@link List} with found {@link Policy} objects
   */
  public List<Policy> find(String query, Integer limit, Integer offset) {
    try {
      var policiesResource = getAuthorizationClient().policies();

      var policies = policiesResource.policies(null, query, null, null, null, false, null, null, offset, limit);
      var foundPolicies = policies.stream()
        .map(keycloakPolicyMapper::toPolicy)
        .filter(Objects::nonNull)
        .toList();

      log.debug("Policies have been found: names = {}", () -> extractPoliciesNames(foundPolicies));
      return foundPolicies;
    } catch (WebApplicationException webApplicationException) {
      var responseStatus = webApplicationException.getResponse().getStatus();
      throw new KeycloakApiException("Failed to search policies", webApplicationException, responseStatus);
    }
  }

  /**
   * Retrieves policy from keycloak by id and converts it to {@link Policy} object.
   *
   * @param id - policy identifier
   * @return {@link Policy} by id
   * @throws KeycloakApiException - if exception occurred during in request
   * @throws NotFoundException - if policy is not found by id
   */
  public Policy getById(UUID id) {
    try {

      var policiesClient = getAuthorizationClient().policies();
      var keycloakPolicyRepresentation = policiesClient.policy(id.toString()).toRepresentation();
      return keycloakPolicyMapper.toPolicy(keycloakPolicyRepresentation);
    } catch (NotFoundException notFoundException) {
      throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, notFoundException);
    } catch (WebApplicationException webApplicationException) {
      var status = webApplicationException.getResponse().getStatus();
      throw new KeycloakApiException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, webApplicationException, status);
    }
  }

  /**
   * Creates a policy in Keycloak.
   *
   * <p>
   * UUID from source policy will be the same as the id of created Keycloak policy.
   * </p>
   *
   * @param policy - policy to create
   * @throws KeycloakApiException if policy is not created
   */
  public void create(Policy policy) {
    var policyRepresentation = keycloakPolicyMapper.toKeycloakPolicy(policy, getPolicyMapperContext(policy));
    var policiesClient = getAuthorizationClient().policies();
    try (var response = policiesClient.create(policyRepresentation)) {
      processKeycloakResponse(policyRepresentation, response);
    }
  }

  /**
   * Updates a policy in Keycloak by identifier.
   *
   * @param policy - policy to update
   * @throws KeycloakApiException if policy was not updated
   */
  public void update(Policy policy) {
    requireNonNull(policy.getId(), "Policy should has ID");
    requireNonNull(policy.getType(), "Policy should has type");
    var policyRepresentation = keycloakPolicyMapper.toKeycloakPolicy(policy, getPolicyMapperContext(policy));

    var policiesClient = getAuthorizationClient().policies();
    var policyId = policy.getId().toString();
    try {
      policiesClient.policy(policyId).update(policyRepresentation);
    } catch (WebApplicationException webApplicationException) {
      var status = webApplicationException.getResponse().getStatus();

      // keycloak returns 500 if policy hasn't been found.
      // It is internal bug in keycloak trying to execute method on null value
      var responseStatus = status == INTERNAL_SERVER_ERROR.value() ? NOT_FOUND.value() : status;
      throw new KeycloakApiException("Failed to update policy", webApplicationException, responseStatus);
    }
  }

  public void deleteById(UUID id) {
    try {
      var policiesClient = getAuthorizationClient().policies();
      policiesClient.policy(id.toString()).remove();
      log.debug("Policy has been deleted: id = {}", id);
    } catch (NotFoundException e) {
      throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, e);
    } catch (WebApplicationException e) {
      throw new KeycloakApiException("Failed to delete policy", e, e.getResponse().getStatus());
    }
  }

  private List<String> extractPoliciesNames(List<Policy> source) {
    return source.stream().map(Policy::getName).collect(toList());
  }

  private PolicyMapperContext getPolicyMapperContext(Policy policy) {
    if (policy.getType() == PolicyType.USER) {
      var userIds = policy.getUserPolicy().getUsers();
      var keycloakUserIds = toStream(userIds)
        .map(userService::findKeycloakIdByUserId)
        .distinct()
        .toList();

      return new PolicyMapperContext().keycloakUserIds(keycloakUserIds);
    }

    return new PolicyMapperContext();
  }

  private AuthorizationResource getAuthorizationClient() {
    return authResourceClientProvider.getAuthorizationClient();
  }

  private static void processKeycloakResponse(PolicyRepresentation policy, Response response) {
    var statusInfo = response.getStatusInfo();
    if (statusInfo.getFamily() == SUCCESSFUL) {
      log.debug("Policy created in Keycloak: id = {}, name = {}", policy.getId(), policy.getName());
      return;
    }

    if (statusInfo.toEnum() == Status.CONFLICT) {
      log.info("Policy already exists in Keycloak [name: {}]", policy.getName());
      return;
    }

    throw new KeycloakApiException(format(
      "Error during scope-based permission creation in Keycloak. Details: status = %s, message = %s",
      statusInfo.getStatusCode(), statusInfo.getReasonPhrase()), null, response.getStatus());
  }
}
