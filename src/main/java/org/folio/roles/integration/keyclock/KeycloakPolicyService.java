package org.folio.roles.integration.keyclock;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CONFLICT;

import feign.FeignException;
import feign.codec.DecodeException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.client.PolicyClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.integration.keyclock.model.policy.UserPolicy;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

/**
 * Keycloak policy service for operations with Keycloak policies.
 */
@Log4j2
@Service
@AllArgsConstructor
public class KeycloakPolicyService {

  private static final String FAILED_TO_FIND_POLICY_ERROR_TEMPLATE = "Failed to find policy: id = ";

  private final PolicyClient client;
  private final KeycloakAccessTokenService tokenService;
  private final KeycloakClientService clientService;
  private final FolioExecutionContext context;
  private final KeycloakPolicyMapper mapper;
  private final KeycloakUserService userService;

  public Policy findById(UUID id) {
    try {
      var keycloakPolicy =
        client.getById(tokenService.getToken(), context.getTenantId(), getClientId(), id);
      return mapper.toPolicy(keycloakPolicy);
    } catch (FeignException.NotFound e) {
      throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, e);
    } catch (FeignException e) {
      throw new KeycloakApiException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, e, e.status());
    }
  }

  public void update(Policy policy) {
    requireNonNull(policy.getId(), "Policy should has ID");
    requireNonNull(policy.getType(), "Policy should has type");
    try {
      var request = mapper.toKeycloakPolicy(policy);
      if (isUserPolicy(policy)) {
        findAndSetKeycloakUserIds((UserPolicy) request);
      }
      var type = request.getType().toLowerCase();
      var policyId = policy.getId();
      client.updateById(tokenService.getToken(), context.getTenantId(), getClientId(), type, policyId,
        request);
      log.debug("Policy has been updated: name = {}, id = {}", policy.getName(), policy.getId());
    } catch (FeignException e) {
      var status = e.status();
      if (e.status() == 500) { //keycloak returns 500 if policy hasn't been found
        status = 404;
      }
      throw new KeycloakApiException("Failed to update policy", e, status);
    }
  }

  public void deleteById(UUID id) {
    try {
      client.deleteById(tokenService.getToken(), context.getTenantId(), getClientId(), id);
      log.debug("Policy has been deleted: id = {}", id);
    } catch (FeignException.NotFound e) {
      throw new EntityNotFoundException(FAILED_TO_FIND_POLICY_ERROR_TEMPLATE + id, e);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to delete policy", e, e.status());
    }
  }

  public List<Policy> search(String query, Integer limit, Integer offset) {
    try {
      var policies =
        client.findAll(tokenService.getToken(), context.getTenantId(), getClientId(), offset, limit,
          query);
      var foundedPolicies = policies.stream().map(mapper::toPolicy).filter(Objects::nonNull).collect(toList());
      log.debug("Policies have been found: names = {}", () -> extractPoliciesNames(foundedPolicies));
      return foundedPolicies;
    } catch (DecodeException e) {
      throw new KeycloakApiException("Cannot map policy type", e, CONFLICT.value());
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to search policies", e, e.status());
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
   */
  public void create(Policy policy) {
    try {
      var request = mapper.toKeycloakPolicy(policy);
      if (isUserPolicy(policy)) {
        findAndSetKeycloakUserIds((UserPolicy) request);
      }
      var type = policy.getType().getValue().toLowerCase();
      var token = tokenService.getToken();

      client.create(token, context.getTenantId(), getClientId(), type, request);
    } catch (FeignException e) {
      processErrorResponse(policy, e);
    }
  }

  private List<String> extractPoliciesNames(List<Policy> source) {
    return source.stream().map(Policy::getName).collect(toList());
  }

  private String getClientId() {
    return clientService.findAndCacheLoginClientUuid();
  }

  private void findAndSetKeycloakUserIds(UserPolicy userPolicy) {
    var keycloakUserIds = userPolicy.getUsers().stream()
      .map(userService::findKeycloakIdByUserId)
      .collect(toList());
    userPolicy.setUsers(keycloakUserIds);
  }

  private static void processErrorResponse(Policy policy, FeignException e) {
    if (e instanceof FeignException.Conflict) {
      log.info("Policy already exists in Keycloak [name: {}]", policy.getName());
      return;
    }

    throw new ServiceException(format(
      "Error during policy creation in Keycloak. Details: status = %s, message = %s",
      e.status(), e.contentUTF8()),
      "policy", policy.getName());
  }

  private static boolean isUserPolicy(Policy policy) {
    return policy.getType() == PolicyType.USER;
  }
}
