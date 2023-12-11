package org.folio.roles.service.policy;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.controller.validation.PolicyValidate;
import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.integration.keyclock.KeycloakPolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

@Log4j2
@Service
@Validated
@RequiredArgsConstructor
public class PolicyService {

  private final KeycloakPolicyService keycloakService;
  private final PolicyEntityService entityService;
  private final TransactionTemplate transactionTemplate;

  /**
   * Finds policy by ID.
   *
   * @param id The ID of the policy to find.
   * @return The {@link Policy} with the specified ID.
   */
  @Transactional(readOnly = true)
  public Policy findById(UUID id) {
    return entityService.findById(id);
  }

  /**
   * Creates a policy.
   *
   * @param policy The {@link Policy} object to be created.
   * @return The created {@link Policy} object.
   */
  @Transactional
  public Policy create(@PolicyValidate Policy policy) {
    var createdPolicy = entityService.create(policy);
    keycloakService.create(createdPolicy);
    return createdPolicy;
  }

  /**
   * Creates one or more policies.
   *
   * @param policies The {@link List} of {@link Policy} objects containing the policies to be created.
   * @return The created {@link Policies}.
   * @throws IllegalStateException if there are no policies to create or policies already created.
   */
  public Policies create(List<@PolicyValidate Policy> policies) {
    var createdPolicies = policies.stream().map(this::createSafe).flatMap(Optional::stream).collect(toList());
    return buildPolicies(createdPolicies);
  }

  /**
   * Updates a policy.
   *
   * @param policy The {@link Policy} to update
   * @return The updated {@link Policy} object
   * @throws IllegalArgumentException if the policy does not have an ID or type
   */
  @Transactional
  public Policy update(@PolicyValidate Policy policy) {
    var updatedPolicy = entityService.update(policy);
    keycloakService.update(policy);
    return updatedPolicy;
  }

  /**
   * Deletes a policy by ID.
   *
   * @param id The ID of the policy to delete.
   */
  @Transactional
  public void deleteById(UUID id) {
    entityService.deleteById(id);

    try {
      keycloakService.deleteById(id);
    } catch (EntityNotFoundException e) {
      log.debug("Policy is not found in Keycloak: id = {}. Nothing to delete", id);
    }
  }

  /**
   * Searches for policies.
   *
   * @return The policies that match the search criteria.
   */
  @Transactional(readOnly = true)
  public Policies search(String query, Integer limit, Integer offset) {
    var policies = entityService.findByQuery(query, offset, limit);
    return buildPolicies(policies);
  }

  /**
   * Searches for policies.
   *
   * @return The policies that match the search criteria.
   */
  @Transactional(readOnly = true)
  public Policy getByNameAndType(String name, PolicyType type) {
    return entityService.findByName(name)
      .filter(policy -> policy.getType() == type)
      .orElseThrow(() -> new EntityNotFoundException(String.format(
        "Role policy is not found by name: %s and type: %s", name, type)));
  }

  /**
   * Checks if policy exists by ID.
   *
   * @param id The ID of the policy to check.
   * @return true if policy exists, false otherwise.
   */
  @Transactional(readOnly = true)
  public boolean existsById(UUID id) {
    return entityService.existsById(id);
  }

  /**
   * Find policy by name and type, otherwise creates it using a newPolicySupplier.
   *
   * @param policyName - policy name to be found
   * @param policyType - policy type to be found
   * @param newPolicySupplier - a new policy dto supplier
   * @return found or created {@link Policy} object
   */
  @Transactional
  public Policy getOrCreatePolicy(String policyName, PolicyType policyType, Supplier<Policy> newPolicySupplier) {
    log.debug("Searching for existing policy related to role: {}", policyName);
    var policyByNameOptional = entityService.findByName(policyName);
    if (policyByNameOptional.isEmpty()) {
      log.info("Policy by name is not found, creating a new one [name: {}]", policyName);
      return create(newPolicySupplier.get());
    }

    var policyByName = policyByNameOptional.get();
    var actualPolicyType = policyByName.getType();
    if (actualPolicyType != policyType) {
      throw new IllegalStateException(format("Type is incorrect for policy: %s, expected: %s but actual is %s",
        policyByName.getId(), policyType, actualPolicyType));
    }

    return policyByName;
  }

  private Optional<Policy> createSafe(Policy policy) {
    try {
      var createdPolicy = transactionTemplate.execute(context -> {
        var createdPolicyEntity = entityService.create(policy);
        keycloakService.create(createdPolicyEntity);
        return createdPolicyEntity;
      });
      return ofNullable(createdPolicy);
    } catch (Exception e) {
      log.warn("Failed to create a policy {}", policy.getName(), e);
      return empty();
    }
  }

  private Policies buildPolicies(List<Policy> policies) {
    if (isNull(policies)) {
      return null;
    }
    return new Policies().policies(policies).totalRecords(policies.size());
  }
}
