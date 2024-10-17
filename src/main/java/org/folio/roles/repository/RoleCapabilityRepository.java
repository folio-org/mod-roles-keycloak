package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleCapabilityRepository extends BaseCqlJpaRepository<RoleCapabilityEntity, RoleCapabilityKey> {

  List<RoleCapabilityEntity> findAllByRoleId(UUID roleId);

  List<RoleCapabilityEntity> findAllByCapabilityId(UUID capabilityId);

  Page<RoleCapabilityEntity> findByRoleId(UUID roleId, Pageable pageable);

  @Query("select rce from RoleCapabilityEntity rce where rce.roleId = :roleId and rce.capabilityId in :ids")
  List<RoleCapabilityEntity> findRoleCapabilities(@Param("roleId") UUID roleId, @Param("ids") List<UUID> ids);

  @Modifying
  @Query("delete from RoleCapabilityEntity rce where rce.roleId = :roleId and rce.capabilityId in :ids")
  void deleteRoleCapabilities(@Param("roleId") UUID userId, @Param("ids") List<UUID> ids);
}
