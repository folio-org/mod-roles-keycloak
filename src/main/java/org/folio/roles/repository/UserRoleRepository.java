package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaCqlRepository<UserRoleEntity, UUID> {

  List<UserRoleEntity> findByUserId(UUID userId);

  List<UserRoleEntity> findByUserIdAndRoleIdIn(UUID userId, List<UUID> roleIds);

  void deleteByUserId(UUID userId);

  void deleteByUserIdAndRoleIdIn(UUID userId, List<UUID> roleIds);
}
