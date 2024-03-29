package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadablePermissionRepository extends BaseCqlJpaRepository<LoadablePermissionEntity, UUID> {

  List<LoadablePermissionEntity> findAllByPermissionNameIn(Collection<String> permissionNames);
}
