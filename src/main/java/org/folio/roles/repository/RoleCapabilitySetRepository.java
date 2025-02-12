package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleCapabilitySetRepository
  extends BaseCqlJpaRepository<RoleCapabilitySetEntity, RoleCapabilitySetKey> {

  @Query("""
    select rcse from RoleCapabilitySetEntity rcse
    inner join CapabilitySetEntity cse on cse.id = rcse.capabilitySetId and cse.dummyCapability = false
    where rcse.roleId = :roleId""")
  List<RoleCapabilitySetEntity> findAllByRoleId(UUID roleId);

  @Query("""
    select rcse from RoleCapabilitySetEntity rcse
    inner join CapabilitySetEntity cse on cse.id = rcse.capabilitySetId and cse.dummyCapability = false
    where rcse.capabilitySetId = :capabilitySetId""")
  List<RoleCapabilitySetEntity> findAllByCapabilitySetId(UUID capabilitySetId);

  @Query("""
    select rcse from RoleCapabilitySetEntity rcse
    inner join CapabilitySetEntity cse on cse.id = rcse.capabilitySetId and cse.dummyCapability = false
    where rcse.roleId = :roleId and rcse.capabilitySetId in (:ids)""")
  List<RoleCapabilitySetEntity> findRoleCapabilitySets(@Param("roleId") UUID roleId, @Param("ids") List<UUID> ids);

  @Query("""
    select rcse from RoleCapabilitySetEntity rcse
    inner join CapabilitySetEntity cse on cse.id = rcse.capabilitySetId and cse.dummyCapability = false
    where rcse.roleId = :roleId""")
  Page<RoleCapabilitySetEntity> findByRoleId(UUID roleId, Pageable pageable);

  @Modifying
  @Query("delete from RoleCapabilitySetEntity rcse where rcse.roleId = :roleId and rcse.capabilitySetId in :ids")
  void deleteRoleCapabilitySets(@Param("roleId") UUID roleId, @Param("ids") List<UUID> ids);
}
