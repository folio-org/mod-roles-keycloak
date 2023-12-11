package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleEntityRepository extends JpaCqlRepository<RoleEntity, UUID> {

  List<RoleEntity> findByIdIn(List<UUID> ids);
}
