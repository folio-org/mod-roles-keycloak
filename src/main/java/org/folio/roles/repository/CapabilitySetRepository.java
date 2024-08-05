package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CapabilitySetRepository extends BaseCqlJpaRepository<CapabilitySetEntity, UUID> {

  @Query(value = """
    SELECT DISTINCT c2.*
    FROM user_role ur
      LEFT JOIN user_capability uc ON uc.user_id = ur.user_id
      LEFT JOIN role_capability rc ON rc.role_id = ur.role_id
      LEFT JOIN capability_set c ON c.id = rc.capability_id OR uc.capability_id = c.id
      LEFT JOIN capability_set c2 ON c.id = ANY (c2.all_parent_ids) OR c2.id = c.id
    WHERE ur.user_id = :userId AND c2.id IS NOT NULL
    """,
    nativeQuery = true)
  List<CapabilitySetEntity> findExpandedCapabilitiesForUser(@Param("userId") UUID userId);

  @Query(value = """
    SELECT DISTINCT c.*
    FROM user_role ur
      LEFT JOIN user_capability uc ON uc.user_id = ur.user_id
      LEFT JOIN role_capability rc ON rc.role_id = ur.role_id
      LEFT JOIN capability_set c ON c.id = rc.capability_id OR uc.capability_id = c.id
    WHERE ur.user_id = :userId AND c.id IS NOT NULL
    """,
    nativeQuery = true)
  List<CapabilitySetEntity> findCapabilitiesForUser(@Param("userId") UUID userId);

  boolean existsByName(String name);

  @Query(nativeQuery = true,
    value = """
      SELECT cs.* FROM capability_set cs
      INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :user_id""",
    countQuery = """
      SELECT COUNT(cs.*) FROM capability_set cs
      INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :user_id""")
  Page<CapabilitySetEntity> findByUserId(@Param("user_id") UUID userId, OffsetRequest offsetRequest);

  @Query(nativeQuery = true,
    value = """
      SELECT cs.* FROM capability_set cs
      INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :role_id""",
    countQuery = """
      SELECT COUNT(cs.*) FROM capability_set cs
      INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :role_id""")
  Page<CapabilitySetEntity> findByRoleId(@Param("role_id") UUID roleId, OffsetRequest offsetRequest);

  @Query("select distinct entity.id from CapabilitySetEntity entity where entity.id in :ids order by entity.id")
  Set<UUID> findCapabilitySetIdsByIdIn(@Param("ids") Collection<UUID> capabilitySetIds);

  Optional<CapabilitySetEntity> findByName(String capabilitySetName);

  List<CapabilitySetEntity> findByNameIn(Collection<String> capabilitySetNames);

  @Query("select entity from CapabilitySetEntity entity where entity.permission in :names order by entity.name")
  List<CapabilitySetEntity> findByPermissionNames(@Param("names") Collection<String> names);

  @Modifying
  @Query(nativeQuery = true, value = "DELETE FROM capability_set_capability WHERE capability_id = :capabilityId")
  void deleteCapabilityCapabilitySetLinks(@Param("capabilityId") UUID capabilityId);

  @Modifying
  @Query("update CapabilitySetEntity cse set cse.applicationId = :applicationId "
    + "where cse.moduleId = :moduleId and cse.applicationId = :oldApplicationId")
  void updateApplicationVersion(@Param("moduleId") String moduleId,
    @Param("applicationId") String applicationId, @Param("oldApplicationId") String oldApplicationId);
}
