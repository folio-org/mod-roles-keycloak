package org.folio.roles.integration.keyclock;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.roles.mapper.KeycloakPolicyMapper.PolicyMapperContext;
import org.folio.spring.FolioExecutionContext;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak policy service for operations with Keycloak policies.
 */
@Log4j2
@Service
@Retryable(
  predicate = KeycloakMethodRetryPredicate.class,
  maxRetriesString = "#{@keycloakConfigurationProperties.retry.maxAttempts}",
  delayString = "#{@keycloakConfigurationProperties.retry.backoff.delayMs}"
)
@AllArgsConstructor
public class KeycloakPolicyService {

  private static final String FAILED_TO_FIND_POLICY_ERROR_TEMPLATE = "Failed to find policy: id = ";

  private final KeycloakUserService userService;
  private final KeycloakPolicyMapper keycloakPolicyMapper;
  private final KeycloakAdminClient keycloakAdminClient;
  private final KeycloakClientService keycloakClientService;
  private final FolioExecutionContext context;

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
      var policies = keycloakAdminClient.findPolicies(realm(), loginClientUuid(), query, false, offset, limit);
      var foundPolicies = policies.stream()
        .map(keycloakPolicyMapper::toPolicy)
        .filter(Objects::nonNull)
        .toList();

      log.debug("Policies have been found: names = {}", () -> extractPoliciesNames(foundPolicies));
      return foundPolicies;
    } catch (RestClientResponseException exception) {
      throw new KeycloakApiException("Failed to search policies", exception, exception.getStatusCode().value());
    }
  }

  /**
   * Retrieves policy from keycloak by id and converts it to {@link Policy} object.
   *
   * @param id - policy identifier
   * @return {@link Policy} by id
   * @throws KeycloakApiException - if exception occurred during in request
   * @throws EntityNotFoundException - if policy is not found by id
   */
  public Policy getById(UUID id) {
    try {
      var keycloakPolicyRepresentation = keycloakAdminClient.getPolicyById(realm(), loginClientUuid(), id.toString());
      return keycloakPolicyMapper.toPolicy(keycloakPolicyRepresentation);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == NOT_FOUND.value()) {
        throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, exception);
      }
      throw new KeycloakApiException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, exception,
        exception.getStatusCode().value());
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
    try {
      keycloakAdminClient.createPolicy(realm(), loginClientUuid(), policyRepresentation);
      log.debug("Policy created in Keycloak: id = {}, name = {}",
        policyRepresentation.getId(), policyRepresentation.getName());
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == CONFLICT.value()) {
        log.info("Policy already exists in Keycloak [name: {}]", policyRepresentation.getName());
        return;
      }
      throw new KeycloakApiException(format(
        "Error during policy creation in Keycloak. Details: id = %s, status = %s, message = %s",
        policyRepresentation.getId(), exception.getStatusCode().value(), exception.getStatusText()),
        exception, exception.getStatusCode().value());
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

    try {
      keycloakAdminClient.updatePolicyById(realm(), loginClientUuid(), policy.getId().toString(),
        policyRepresentation);
    } catch (RestClientResponseException exception) {
      var status = exception.getStatusCode().value();

      // keycloak returns 500 if policy hasn't been found.
      // It is internal bug in keycloak trying to execute method on null value
      var responseStatus = status == INTERNAL_SERVER_ERROR.value() ? NOT_FOUND.value() : status;
      throw new KeycloakApiException("Failed to update policy", exception, responseStatus);
    }
  }

  public void deleteById(UUID id) {
    try {
      keycloakAdminClient.deletePolicyById(realm(), loginClientUuid(), id.toString());
      log.debug("Policy has been deleted: id = {}", id);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == NOT_FOUND.value()) {
        throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, exception);
      }
      throw new KeycloakApiException("Failed to delete policy", exception, exception.getStatusCode().value());
    }
  }

  private List<String> extractPoliciesNames(List<Policy> source) {
    return source.stream().map(Policy::getName).toList();
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

  private String realm() {
    return context.getTenantId();
  }

  private String loginClientUuid() {
    return keycloakClientService.getLoginClient().getId();
  }
}
