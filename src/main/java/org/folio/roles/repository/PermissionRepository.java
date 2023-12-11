package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.PermissionEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends BaseCqlJpaRepository<PermissionEntity, UUID> {

  List<PermissionEntity> findByPermissionNameIn(Collection<String> names);
}
