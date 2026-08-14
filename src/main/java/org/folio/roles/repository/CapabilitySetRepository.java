package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.repository.projection.UserCapabilitySetNameProjection;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CapabilitySetRepository extends BaseCqlJpaRepository<CapabilitySetEntity, UUID> {

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

  /**
   * Returns effective capability-set names for the given users: sets assigned directly and sets
   * inherited through assigned roles.
   *
   * <p>{@code UNION} (not {@code UNION ALL}) is the enforcement point for "a capability-set name is
   * returned once per user" - the callers rely on it and do not deduplicate again. The user ids are
   * bound as a single array rather than an expanded {@code IN} list so the statement text stays
   * constant regardless of batch size.</p>
   *
   * @param userIds user identifiers to look up
   * @return user id / capability-set name pairs, unordered
   */
  @Query(nativeQuery = true,
    value = """
      SELECT ucs.user_id AS userId, cs.name AS capabilitySetName
      FROM user_capability_set ucs
      INNER JOIN capability_set cs ON cs.id = ucs.capability_set_id
      WHERE ucs.user_id = ANY(:userIds)
      UNION
      SELECT ur.user_id AS userId, cs.name AS capabilitySetName
      FROM user_role ur
      INNER JOIN role_capability_set rcs ON rcs.role_id = ur.role_id
      INNER JOIN capability_set cs ON cs.id = rcs.capability_set_id
      WHERE ur.user_id = ANY(:userIds)""")
  List<UserCapabilitySetNameProjection> findEffectiveCapabilitySetNames(@Param("userIds") UUID[] userIds);

  /**
   * Returns effective capability-set names for the given users, restricted to an exact-name whitelist.
   *
   * <p>The name filter is repeated inside both union branches on purpose: as a hard predicate it lets
   * PostgreSQL drive the join from the unique {@code capability_set(name)} index. Folding both variants
   * into one query with a {@code (:names IS NULL OR ...)} guard would take that plan away.</p>
   *
   * @param userIds user identifiers to look up
   * @param names capability-set names to keep; an empty array matches nothing
   * @return user id / capability-set name pairs, unordered
   */
  @Query(nativeQuery = true,
    value = """
      SELECT ucs.user_id AS userId, cs.name AS capabilitySetName
      FROM user_capability_set ucs
      INNER JOIN capability_set cs ON cs.id = ucs.capability_set_id
      WHERE ucs.user_id = ANY(:userIds) AND cs.name = ANY(:names)
      UNION
      SELECT ur.user_id AS userId, cs.name AS capabilitySetName
      FROM user_role ur
      INNER JOIN role_capability_set rcs ON rcs.role_id = ur.role_id
      INNER JOIN capability_set cs ON cs.id = rcs.capability_set_id
      WHERE ur.user_id = ANY(:userIds) AND cs.name = ANY(:names)""")
  List<UserCapabilitySetNameProjection> findEffectiveCapabilitySetNames(
    @Param("userIds") UUID[] userIds, @Param("names") String[] names);

  @Query("select entity from CapabilitySetEntity entity where entity.permission in :names order by entity.name")
  List<CapabilitySetEntity> findByPermissionNames(@Param("names") Collection<String> names);

  Optional<CapabilitySetEntity> findByPermission(String permissionName);

  @Query(nativeQuery = true,
    value = """
      SELECT cs.* FROM capability_set cs
      INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
      INNER JOIN capability c ON c.id = csc.capability_id
      WHERE c.name = :capabilityName""")
  List<CapabilitySetEntity> findByCapabilityName(@Param("capabilityName") String capabilityName);

  @Query(nativeQuery = true,
    value = """
      SELECT cs.* FROM capability_set cs
      INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
      WHERE csc.capability_id = :capabilityId""")
  List<CapabilitySetEntity> findAllByCapabilityId(UUID capabilityId);

  @Modifying
  @Query(nativeQuery = true, value = "DELETE FROM capability_set_capability WHERE capability_id = :capabilityId")
  void deleteCapabilityCapabilitySetLinks(@Param("capabilityId") UUID capabilityId);

  @Modifying
  @Query("UPDATE CapabilitySetEntity cse SET cse.applicationId = :applicationId "
    + "WHERE cse.moduleId = :moduleId AND cse.applicationId = :oldApplicationId")
  void updateApplicationVersion(@Param("moduleId") String moduleId,
    @Param("applicationId") String applicationId, @Param("oldApplicationId") String oldApplicationId);

  @Modifying
  @Query("UPDATE CapabilitySetEntity cse SET cse.applicationId = :newApplicationId, cse.moduleId = :newModuleId "
    + " WHERE cse.moduleId LIKE CONCAT(:moduleName, '-%') AND cse.applicationId LIKE CONCAT(:applicationName, '-%')")
  void updateAppAndModuleVersionByAppAndModuleName(@Param("applicationName") String applicationName,
    @Param("moduleName") String moduleName, @Param("newApplicationId") String newApplicationId,
    @Param("newModuleId") String newModuleId);

  @Modifying
  @Query(nativeQuery = true,
    value = """
      INSERT INTO capability_set_capability (capability_set_id, capability_id)
      VALUES (:capabilitySetId, :capabilityId) ON CONFLICT DO NOTHING""")
  void addCapabilityById(@Param("capabilitySetId") UUID capabilitySetId, @Param("capabilityId") UUID capabilityId);
}
