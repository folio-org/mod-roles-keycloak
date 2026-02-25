package org.folio.roles.service.reference;

import static java.util.stream.Collectors.toSet;
import static org.folio.common.utils.CollectionUtils.toStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.service.policy.PolicyEntityService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.utils.ResourceHelper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class PoliciesDataLoader implements ReferenceDataLoader {

  private static final String POLICIES_DATA_DIR = BASE_DIR + "policies";

  private final PolicyService policyService;
  private final PolicyEntityService policyEntityService;
  private final ResourceHelper resourceHelper;

  @Override
  public void loadReferenceData() {
    var preparedPolicies = resourceHelper.readObjectsFromDirectory(POLICIES_DATA_DIR, Policies.class)
      .flatMap(policies -> toStream(policies.getPolicies()))
      .collect(toSet());

    for (Policy policy : preparedPolicies) {
      var existedPolicyOpt = policyEntityService.findByName(policy.getName());
      if (existedPolicyOpt.isPresent()) {
        policy.setId(existedPolicyOpt.get().getId());
        var updatedPolicy = policyService.update(policy);
        log.info("Policy has been updated: id = {}, name = {}", updatedPolicy.getId(), updatedPolicy.getName());
      } else {
        var createdPolicy = policyService.create(policy);
        log.info("Policy has been created: id = {}, name = {}", createdPolicy.getId(), createdPolicy.getName());
      }
    }
  }
}
