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
  Page<CapabilityEntity> findByCapabilitySetId(@Param("capabilitySetId") UUID capabilitySetId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN user_capability uc ON c.id = uc.capability_id AND uc.user_id = :userId""")
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
  Page<CapabilityEntity> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query(nativeQuery = true,
    value = """
      SELECT c.* FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId""",
    countQuery = """
      SELECT COUNT(c.*) FROM capability c
      INNER JOIN role_capability rc ON c.id = rc.capability_id AND rc.role_id = :roleId""")
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
  Page<CapabilityEntity> findAllByRoleId(@Param("roleId") UUID roleId, Pageable pageable);

  @Query("select entity from CapabilityEntity entity where entity.name in :names order by entity.name")
  List<CapabilityEntity> findAllByNames(@Param("names") Collection<String> names);

  @Query("select entity from CapabilityEntity entity where entity.name = :name")
  Optional<CapabilityEntity> findByName(@Param("name") String name);

  @Query("select distinct entity.id from CapabilityEntity entity where entity.id in :ids order by entity.id")
  Set<UUID> findCapabilityIdsByIdIn(@Param("ids") Collection<UUID> capabilityIds);

  @Query(nativeQuery = true, value = """
    SELECT c.* FROM capability c
      INNER JOIN capability_set_capability csc
      ON c.id = csc.capability_id AND csc.capability_set_id IN (:ids)""")
  List<CapabilityEntity> findByCapabilitySetIds(@Param("ids") Collection<UUID> capabilitySetIds);

  @Query(nativeQuery = true, value = """
    WITH prefixes AS (
        SELECT prefix || '%' AS pattern
        FROM UNNEST(cast(:prefixes as text[])) prefix
    ),
    user_capabilities AS (
        SELECT uc.user_id, uc.capability_id
        FROM user_capability uc

        UNION ALL

        SELECT ucs.user_id, csc.capability_id
        FROM user_capability_set ucs
        INNER JOIN capability_set_capability csc
            ON ucs.capability_set_id = csc.capability_set_id

        UNION ALL

        SELECT ur.user_id, rc.capability_id
        FROM user_role ur
        INNER JOIN (
            SELECT rc.role_id, rc.capability_id
            FROM role_capability rc

            UNION ALL

            SELECT rcs.role_id, csc.capability_id
            FROM role_capability_set rcs
            INNER JOIN capability_set_capability csc
                ON rcs.capability_set_id = csc.capability_set_id
        ) rc ON rc.role_id = ur.role_id
    ),
    user_permissions AS (
        SELECT DISTINCT c.folio_permission
        FROM user_capabilities uc
    INNER JOIN capability c ON uc.capability_id = c.id
        LEFT JOIN prefixes p ON c.folio_permission LIKE p.pattern
        WHERE uc.user_id = :user_id
          AND p.pattern IS NOT NULL
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
    ) combined_permissions;

    """)
  List<String> findPermissionsByPrefixes(@Param("user_id") UUID userId, @Param("prefixes") String prefixes);

  @Query(nativeQuery = true, value = """
    WITH user_permissions AS (
      SELECT c.folio_permission
      FROM (
        SELECT uc.user_id, uc.capability_id
        FROM user_capability uc

        UNION ALL

        SELECT ucs.user_id, csc.capability_id
        FROM user_capability_set ucs
        INNER JOIN capability_set_capability csc
          ON ucs.capability_set_id = csc.capability_set_id

        UNION ALL

        SELECT ur.user_id, rc.capability_id
        FROM user_role ur
        INNER JOIN (
          SELECT rc.role_id, rc.capability_id
          FROM role_capability rc

          UNION ALL

          SELECT rcs.role_id, csc.capability_id
          FROM role_capability_set rcs
          INNER JOIN capability_set_capability csc
            ON rcs.capability_set_id = csc.capability_set_id
        ) rc
          ON rc.role_id = ur.role_id
      ) uc
      INNER JOIN capability c
        ON uc.capability_id = c.id
      WHERE uc.user_id = :user_id
    ),
    replaced_permissions AS (
      SELECT UNNEST(p.replaces) AS folio_permission
      FROM user_permissions up
      INNER JOIN permission p
        ON p.name = up.folio_permission
    )
    SELECT folio_permission
    FROM (
      SELECT folio_permission FROM user_permissions
      UNION ALL
      SELECT folio_permission FROM replaced_permissions
    ) all_permissions
    GROUP BY folio_permission;
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

  @Query("select entity from CapabilityEntity entity where entity.permission in :names order by entity.name")
  List<CapabilityEntity> findAllByPermissionNames(@Param("names") Collection<String> names);

  @Query(nativeQuery = true, value = """
    WITH prefixes AS (
      SELECT prefix || '%' AS pattern
      FROM UNNEST(CAST(:prefixes AS text[])) AS prefix
    ),
    permission_names AS (
      SELECT permission_name
      FROM UNNEST(CAST(:permission_names AS text[])) AS permission_name
    ),
    user_capabilities AS (
      SELECT uc.user_id, uc.capability_id FROM user_capability uc

      UNION ALL

      SELECT ucs.user_id, csc.capability_id
      FROM user_capability_set ucs
      INNER JOIN capability_set_capability csc
        ON ucs.capability_set_id = csc.capability_set_id

      UNION ALL

      SELECT ur.user_id, rc.capability_id
      FROM user_role ur
      INNER JOIN (
        SELECT rc.role_id, rc.capability_id FROM role_capability rc

        UNION ALL

        SELECT rcs.role_id, csc.capability_id
        FROM role_capability_set rcs
        INNER JOIN capability_set_capability csc
          ON rcs.capability_set_id = csc.capability_set_id
      ) rc ON rc.role_id = ur.role_id
    ),
    user_permissions AS (
      SELECT DISTINCT c.folio_permission
      FROM user_capabilities uc
      INNER JOIN capability c ON uc.capability_id = c.id
      WHERE uc.user_id = :user_id
        AND (
          c.folio_permission IN (SELECT permission_name FROM permission_names)
          OR EXISTS (
            SELECT 1 FROM prefixes p
            WHERE c.folio_permission LIKE p.pattern
          )
        )
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
    ) t;
    """)
  List<String> findPermissionsByPrefixesAndPermissionNames(@Param("user_id") UUID userId,
    @Param("permission_names") String permissionNames, @Param("prefixes") String prefixes);
}
