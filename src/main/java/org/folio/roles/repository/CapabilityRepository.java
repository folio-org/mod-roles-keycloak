package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CapabilityRepository extends BaseCqlJpaRepository<CapabilityEntity, UUID> {

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id = :capabilitySetId""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id = :capabilitySetId""")
  Page<CapabilityEntity> findByCapabilitySetIdIncludeDummy(@Param("capabilitySetId") UUID capabilitySetId,
    Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id = :capabilitySetId WHERE c.dummy_capability = false""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id = :capabilitySetId WHERE c.dummy_capability = false""")
  Page<CapabilityEntity> findByCapabilitySetId(@Param("capabilitySetId") UUID capabilitySetId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId""")
  Page<CapabilityEntity> findByUserIdIncludeDummy(@Param("userId") UUID userId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId
      WHERE c.dummy_capability = false""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId
      WHERE c.dummy_capability = false""")
  Page<CapabilityEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT DISTINCT capability.* FROM (
        SELECT c.* FROM capability c
        INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId

        UNION

        SELECT c.* FROM capability_set cs
        INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :userId
        INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
        INNER JOIN capability c ON csc.capability_id = c.id
      ) capability""",
    countQuery = """
      SELECT COUNT(DISTINCT capability.*) FROM (
        SELECT c.* FROM capability c
        INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId

        UNION

        SELECT c.* FROM capability_set cs
        INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :userId
        INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
        INNER JOIN capability c ON csc.capability_id = c.id
      ) capability""")
  Page<CapabilityEntity> findAllByUserIdIncludeDummy(@Param("userId") UUID userId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT DISTINCT capability.* FROM (
        SELECT c.* FROM capability c
        INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId

        UNION

        SELECT c.* FROM capability_set cs
        INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :userId
        INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
        INNER JOIN capability c ON csc.capability_id = c.id
      ) capability WHERE capability.dummy_capability = false""",
    countQuery = """
      SELECT COUNT(DISTINCT capability.*) FROM (
        SELECT c.* FROM capability c
        INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId

        UNION

        SELECT c.* FROM capability_set cs
        INNER JOIN user_capability_set ucs ON cs.id = ucs.capability_set_id AND ucs.user_id = :userId
        INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
        INNER JOIN capability c ON csc.capability_id = c.id
      ) capability WHERE capability.dummy_capability = false""")
  Page<CapabilityEntity> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId""")
  Page<CapabilityEntity> findByRoleIdIncludeDummy(@Param("roleId") UUID roleId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId
      WHERE c.dummy_capability = false""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId
      WHERE c.dummy_capability = false""")
  Page<CapabilityEntity> findByRoleId(@Param("roleId") UUID roleId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT DISTINCT capability.* FROM (
        SELECT c.* FROM capability c
          INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId

        UNION

        SELECT c.* FROM capability_set cs
          INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :roleId
          INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
          INNER JOIN capability c ON csc.capability_id = c.id
      ) capability""",
    countQuery = """
      SELECT COUNT(DISTINCT capability.*) FROM (
        SELECT c.* FROM capability c
          INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId

        UNION

        SELECT c.* FROM capability_set cs
          INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :roleId
          INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
          INNER JOIN capability c ON csc.capability_id = c.id
      ) capability""")
  Page<CapabilityEntity> findAllByRoleIdIncludeDummy(@Param("roleId") UUID roleId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT DISTINCT capability.* FROM (
        SELECT c.* FROM capability c
          INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId

        UNION

        SELECT c.* FROM capability_set cs
          INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :roleId
          INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
          INNER JOIN capability c ON csc.capability_id = c.id
      ) capability WHERE capability.dummy_capability = false""",
    countQuery = """
      SELECT COUNT(DISTINCT capability.*) FROM (
        SELECT c.* FROM capability c
          INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId

        UNION

        SELECT c.* FROM capability_set cs
          INNER JOIN role_capability_set rcs ON cs.id = rcs.capability_set_id AND rcs.role_id = :roleId
          INNER JOIN capability_set_capability csc ON cs.id = csc.capability_set_id
          INNER JOIN capability c ON csc.capability_id = c.id
      ) capability WHERE capability.dummy_capability = false""")
  Page<CapabilityEntity> findAllByRoleId(@Param("roleId") UUID roleId, Pageable pageable);

  @Query("""
    select entity from CapabilityEntity entity where entity.name in :names
    and entity.dummyCapability = false order by entity.name""")
  List<CapabilityEntity> findAllByNames(@Param("names") Collection<String> names);

  @Query("""
    select entity from CapabilityEntity entity where entity.name in :names
    order by entity.name""")
  List<CapabilityEntity> findAllByNamesIncludeDummy(@Param("names") Collection<String> names);

  @Query("select entity from CapabilityEntity entity where entity.name = :name and entity.dummyCapability=false")
  Optional<CapabilityEntity> findByName(@Param("name") String name);

  @Query("""
    select distinct entity.id from CapabilityEntity entity where entity.id in :ids
    order by entity.id""")
  Set<UUID> findCapabilityIdsByIdIncludeDummy(@Param("ids") Collection<UUID> capabilityIds);

  @Query(nativeQuery = true, value = """
    SELECT c.* FROM capability c
      INNER JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id IN (:ids) WHERE c.dummy_capability = false""")
  List<CapabilityEntity> findByCapabilitySetIds(@Param("ids") Collection<UUID> capabilitySetIds);

  @Query(nativeQuery = true, value = """
    SELECT c.* FROM capability c
      INNER JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id IN (:ids)""")
  List<CapabilityEntity> findByCapabilitySetIdsIncludeDummy(@Param("ids") Collection<UUID> capabilitySetIds);

  @Query(nativeQuery = true, value = """
    WITH user_permissions AS (
      -- Direct user capabilities (filter early)
      SELECT c.folio_permission
      FROM user_capability uc
      INNER JOIN capability c ON uc.capability_id = c.id
      WHERE uc.user_id = :user_id
        AND c.dummy_capability = false

      UNION

      -- User capability sets (filter early)
      SELECT c.folio_permission
      FROM user_capability_set ucs
      INNER JOIN capability_set_capability csc ON ucs.capability_set_id = csc.capability_set_id
      INNER JOIN capability c ON csc.capability_id = c.id
      WHERE ucs.user_id = :user_id
        AND c.dummy_capability = false

      UNION

      -- Role capabilities via user_role (filter early on user_id)
      SELECT c.folio_permission
      FROM user_role ur
      INNER JOIN role_capability rc ON ur.role_id = rc.role_id
      INNER JOIN capability c ON rc.capability_id = c.id
      WHERE ur.user_id = :user_id
        AND c.dummy_capability = false

      UNION

      -- Role capability sets via user_role (filter early on user_id)
      SELECT c.folio_permission
      FROM user_role ur
      INNER JOIN role_capability_set rcs ON ur.role_id = rcs.role_id
      INNER JOIN capability_set_capability csc ON rcs.capability_set_id = csc.capability_set_id
      INNER JOIN capability c ON csc.capability_id = c.id
      WHERE ur.user_id = :user_id
        AND c.dummy_capability = false
    ),
    replaced_permissions AS (
      SELECT UNNEST(p.replaces) AS folio_permission
      FROM user_permissions up
      INNER JOIN permission p
        ON p.name = up.folio_permission
    )
    SELECT DISTINCT folio_permission
    FROM (
      SELECT folio_permission FROM user_permissions
      UNION ALL
      SELECT folio_permission FROM replaced_permissions
    ) all_permissions;
    """)
  List<String> findAllFolioPermissions(@Param("user_id") UUID userId);

  @Modifying
  @Query("update CapabilityEntity ce set ce.applicationId = :applicationId "
    + "where ce.moduleId = :moduleId and ce.applicationId = :oldApplicationId")
  void updateApplicationVersion(@Param("moduleId") String moduleId,
    @Param("applicationId") String applicationId, @Param("oldApplicationId") String oldApplicationId);

  @Modifying
  @Query("update CapabilityEntity ce set ce.applicationId = :applicationId, ce.moduleId = :newModuleId "
    + "where ce.moduleId LIKE CONCAT(:moduleName, '-%') and ce.applicationId LIKE CONCAT(:applicationName, '-%')")
  void updateAppAndModuleVersionByAppAndModuleName(@Param("applicationName") String applicationName,
    @Param("moduleName") String moduleName, @Param("applicationId") String newApplicationId,
    @Param("newModuleId") String newModuleId);

  @Query("""
    select entity from CapabilityEntity entity where entity.permission in :names
    and entity.dummyCapability = false
    order by entity.name""")
  List<CapabilityEntity> findAllByPermissionNames(@Param("names") Collection<String> names);

  @Query("""
    select entity from CapabilityEntity entity where entity.permission in :names
    order by entity.name""")
  List<CapabilityEntity> findAllByPermissionNamesIncludeDummy(@Param("names") Collection<String> names);

  Optional<CapabilityEntity> findByPermission(String permissionName);

  @Query("select entity.name from CapabilityEntity entity where entity.name in :names and dummyCapability=true")
  List<String> findDummyCapabilitiesByNames(@Param("names") Collection<String> names);
}
