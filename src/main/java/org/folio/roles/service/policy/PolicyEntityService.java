package org.folio.roles.service.policy;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.mapper.entity.PolicyEntityMapper;
import org.folio.roles.repository.PolicyEntityRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class PolicyEntityService {

  private final PolicyEntityRepository repository;
  private final PolicyEntityMapper mapper;

  /**
   * Saves a new Policy record in the database.
   *
   * @param policy object {@link Policy} to save.
   * @return a saved {@link Policy} in database.
   */
  public Policy create(Policy policy) {
    var policyName = policy.getName();
    var policyByName = repository.findByName(policyName);
    if (policyByName.isPresent()) {
      throw new EntityExistsException("Policy name is taken: " + policyName);
    }

    var entity = mapper.toPolicyEntity(policy);
    var createdEntity = repository.save(entity);
    log.debug("Policy created: id = {}, name = {}, type = {}", createdEntity.getId(), policyName, policy.getType());
    return mapper.toPolicy(createdEntity);
  }

  public Policy update(Policy policy) {
    requireNonNull(policy.getId(), "To update policy, policy ID cannot be null");
    checkIfPolicyExists(policy.getId());
    var updatedPolicy = repository.save(mapper.toPolicyEntity(policy));
    log.debug("Policy has been updated: id = {}", policy.getId());
    return mapper.toPolicy(updatedPolicy);
  }

  public void deleteById(UUID id) {
    repository.deleteById(id);
    log.debug("Policy has been deleted: id = {}", id);
  }

  @Transactional(readOnly = true)
  public Policy findById(UUID id) {
    var policyEntity =
      repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Policy not found: id = " + id));
    log.debug("Policy has been found: id = {}", id);
    return mapper.toPolicy(policyEntity);
  }

  @Transactional(readOnly = true)
  public List<Policy> findByQuery(String query, Integer offset, Integer limit) {
    var offsetRequest = OffsetRequest.of(offset, limit);
    var policyEntities = isNotBlank(query)
      ? repository.findByCql(query, offsetRequest)
      : repository.findAll(offsetRequest);
    log.debug("Policies have been found: count = {}", policyEntities.getTotalElements());
    return mapper.toPolicy(policyEntities.getContent());
  }

  /**
   * Retrieves role policies by capability id.
   *
   * @param capabilityId - capability identifier
   * @return {@link List} with found role {@link Policy} by capability id
   */
  @Transactional(readOnly = true)
  public List<Policy> findRolePoliciesByCapabilityId(UUID capabilityId) {
    return mapItems(repository.findRolePoliciesByCapabilityId(capabilityId), mapper::toRolePolicy);
  }

  /**
   * Retrieves role policies by capability set id.
   *
   * @param capabilitySetId - capability set identifier
   * @return {@link List} with found role {@link Policy} by capability set id
   */
  @Transactional(readOnly = true)
  public List<Policy> findRolePoliciesByCapabilitySetId(UUID capabilitySetId) {
    return mapItems(repository.findRolePoliciesByCapabilitySetId(capabilitySetId), mapper::toRolePolicy);
  }

  /**
   * Retrieves user policies by capability id.
   *
   * @param capabilityId - capability identifier
   * @return {@link List} with found user {@link Policy} by capability id
   */
  @Transactional(readOnly = true)
  public List<Policy> findUserPoliciesByCapabilityId(UUID capabilityId) {
    return mapItems(repository.findUserPoliciesByCapabilityId(capabilityId), mapper::toUserPolicy);
  }

  /**
   * Retrieves user policies by capability set id.
   *
   * @param capabilitySetId - capability set identifier
   * @return {@link List} with found user {@link Policy} by capability set id
   */
  @Transactional(readOnly = true)
  public List<Policy> findUserPoliciesByCapabilitySetId(UUID capabilitySetId) {
    return mapItems(repository.findUserPoliciesByCapabilitySetId(capabilitySetId), mapper::toUserPolicy);
  }

  @Transactional(readOnly = true)
  public Optional<Policy> findByName(String name) {
    return repository.findByName(name).map(mapper::toPolicy);
  }

  @Transactional(readOnly = true)
  public boolean existsById(UUID id) {
    return repository.existsById(id);
  }

  private void checkIfPolicyExists(UUID id) {
    repository.getReferenceById(id);
  }
}
