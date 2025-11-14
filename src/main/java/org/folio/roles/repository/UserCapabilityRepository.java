package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.key.UserCapabilityKey;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCapabilityRepository extends BaseCqlJpaRepository<UserCapabilityEntity, UserCapabilityKey> {

  @Query("""
    select uce from UserCapabilityEntity uce
    inner join CapabilityEntity ce on ce.id = uce.capabilityId and ce.dummyCapability = false
    where uce.userId = :userId""")
  List<UserCapabilityEntity> findAllByUserId(@Param("userId") UUID userId);

  @Query("""
    select uce from UserCapabilityEntity uce
    inner join CapabilityEntity ce on ce.id = uce.capabilityId and ce.dummyCapability = false
    where uce.capabilityId  = :capabilityId""")
  List<UserCapabilityEntity> findAllByCapabilityId(@Param("capabilityId") UUID capabilityId);

  @Query("""
    select uce from UserCapabilityEntity uce
    inner join  CapabilityEntity ce on ce.id = uce.capabilityId and ce.dummyCapability = false
    where uce.userId = :userId and uce.capabilityId in :ids""")
  List<UserCapabilityEntity> findUserCapabilities(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

  @Modifying
  @Query("delete from UserCapabilityEntity uce where uce.userId = :userId and uce.capabilityId in :ids")
  void deleteUserCapabilities(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

  boolean existsByUserIdAndCapabilityId(UUID userId, UUID capabilityId);
}
