package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.PermissionEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends BaseCqlJpaRepository<PermissionEntity, UUID> {

  List<PermissionEntity> findByPermissionNameIn(Collection<String> names);

  @Modifying
  void deleteAllByPermissionNameIn(Collection<String> permissionNames);

  @Query(nativeQuery = true, value = """
    WITH RECURSIVE permission_hierarchy AS (
      SELECT id, name, sub_permissions
      FROM permission
      WHERE name = :permissionName
      UNION
      SELECT p.id, p.name, p.sub_permissions
      FROM permission p
             JOIN permission_hierarchy ph ON p.sub_permissions @> ARRAY [ph.name]
    )
    SELECT name
    FROM permission_hierarchy
    WHERE name != :permissionName
    """)
  List<String> getAllParentPermissions(@Param("permissionName") String permissionName);
}
