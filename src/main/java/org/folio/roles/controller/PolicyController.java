package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.PoliciesRequest;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.rest.resource.PoliciesApi;
import org.folio.roles.service.policy.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PolicyController implements PoliciesApi {

  private final PolicyService policyService;

  @Override
  public ResponseEntity<Policy> createPolicy(Policy policy) {
    var createdPolicy = policyService.create(policy);
    return ResponseEntity.status(CREATED).body(createdPolicy);
  }

  @Override
  public ResponseEntity<Policies> createPolicies(PoliciesRequest policiesRequest) {
    var policies = policyService.create(policiesRequest.getPolicies());
    return ResponseEntity.status(CREATED).body(policies);
  }

  @Override
  public ResponseEntity<Policies> findPolicies(String query, Integer limit, Integer offset) {
    var policies = policyService.search(query, limit, offset);
    return ResponseEntity.ok(policies);
  }

  @Override
  public ResponseEntity<Void> deletePolicy(UUID id) {
    policyService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Policy> getPolicy(UUID id) {
    var policy = policyService.findById(id);
    return ResponseEntity.ok(policy);
  }

  @Override
  public ResponseEntity<Void> updatePolicy(UUID id, Policy policy) {
    policy.setId(id);
    policyService.update(policy);
    return ResponseEntity.noContent().build();
  }
}
