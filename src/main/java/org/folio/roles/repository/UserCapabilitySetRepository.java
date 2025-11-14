package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.key.UserCapabilitySetKey;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCapabilitySetRepository
  extends BaseCqlJpaRepository<UserCapabilitySetEntity, UserCapabilitySetKey> {

  List<UserCapabilitySetEntity> findAllByUserId(UUID userId);

  List<UserCapabilitySetEntity> findAllByCapabilitySetId(UUID capabilitySetId);

  @Query("""
    select entity from UserCapabilitySetEntity entity
      where entity.userId = :userId
      and entity.capabilitySetId in (:capabilitySetIds)""")
  List<UserCapabilitySetEntity> findUserCapabilitySets(
    @Param("userId") UUID roleId,
    @Param("capabilitySetIds") List<UUID> capabilitySetIds);

  @Modifying
  @Query("delete from UserCapabilitySetEntity uce where uce.userId = :userId and uce.capabilitySetId in :ids")
  void deleteUserCapabilitySets(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

  boolean existsByUserIdAndCapabilitySetId(UUID userId, UUID capabilitySetId);
}
