package org.folio.roles.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyEntityRepository extends JpaCqlRepository<BasePolicyEntity, UUID> {

  Optional<BasePolicyEntity> findByName(String name);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT p.* FROM capability_set cs
      JOIN role_capability_set rcs ON rcs.capability_set_id = cs.id
      JOIN policy_roles pr ON pr.role_id = rcs.role_id
      JOIN policy p ON pr.policy_id = p.id AND p.type = 'ROLE'
    WHERE cs.id = :capability_set_id""")
  List<RolePolicyEntity> findRolePoliciesByCapabilitySetId(@Param("capability_set_id") UUID capabilitySetId);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT p.* FROM capability_set cs
      JOIN user_capability_set ucs ON ucs.capability_set_id = cs.id
      JOIN policy_users pu ON pu.user_id = ucs.user_id
      JOIN policy p ON pu.policy_id = p.id AND p.type = 'USER'
    WHERE cs.id = :capability_set_id""")
  List<UserPolicyEntity> findUserPoliciesByCapabilitySetId(@Param("capability_set_id") UUID capabilitySetId);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT policy.* FROM (
      SELECT DISTINCT p.* FROM capability c
        JOIN role_capability rc ON rc.capability_id = c.id
        JOIN policy_roles pr ON pr.role_id = rc.role_id
        JOIN policy p ON pr.policy_id = p.id AND p.type = 'ROLE'
      WHERE c.id = :capabilityId AND c.dummy_capability = false

      UNION

      SELECT DISTINCT p.* FROM capability c
      JOIN capability_set_capability csc ON c.id = csc.capability_id
        JOIN role_capability_set rcs ON rcs.capability_set_id = csc.capability_set_id
        JOIN policy_roles pr ON pr.role_id = rcs.role_id
        JOIN policy p ON pr.policy_id = p.id AND p.type = 'ROLE'
      WHERE c.id = :capabilityId AND c.dummy_capability = false) policy""")
  List<RolePolicyEntity> findRolePoliciesByCapabilityId(@Param("capabilityId") UUID capabilityId);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT policy.* FROM (
      SELECT DISTINCT p.* FROM capability c
        JOIN user_capability uc ON uc.capability_id = c.id
        JOIN policy_users pu ON pu.user_id = uc.user_id
        JOIN policy p ON pu.policy_id = p.id AND p.type = 'USER'
      WHERE c.id = :capabilityId AND c.dummy_capability = false

      UNION

      SELECT DISTINCT p.* FROM capability c
      JOIN capability_set_capability csc ON c.id = csc.capability_id
        JOIN user_capability_set ucs ON ucs.capability_set_id = csc.capability_set_id
        JOIN policy_users pu ON pu.user_id = ucs.user_id
        JOIN policy p ON pu.policy_id = p.id AND p.type = 'USER'
      WHERE c.id = :capabilityId AND c.dummy_capability = false) policy""")
  List<UserPolicyEntity> findUserPoliciesByCapabilityId(@Param("capabilityId") UUID capabilityId);
}
