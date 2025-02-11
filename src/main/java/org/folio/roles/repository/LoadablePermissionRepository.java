package org.folio.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadablePermissionRepository
  extends BaseCqlJpaRepository<LoadablePermissionEntity, LoadablePermissionKey> {

  List<LoadablePermissionEntity> findAllByPermissionNameIn(Collection<String> permissionNames);

  @Query(nativeQuery = true,
    value = """
      SELECT rlp.* FROM role_loadable_permission rlp
      INNER JOIN capability c
      ON c.id = rlp.capability_id AND rlp.capability_id = :capabilityId
      AND c.dummy_capability = false""")
  Stream<LoadablePermissionEntity> findAllByCapabilityId(UUID capabilityId);

  @Query(nativeQuery = true,
    value = """
      SELECT rlp.* FROM role_loadable_permission rlp
      INNER JOIN capability_set cs
      ON cs.id = rlp.capability_set_id AND rlp.capability_set_id = :capabilitySetId
      AND cs.dummy_capability = false""")
  Stream<LoadablePermissionEntity> findAllByCapabilitySetId(UUID capabilitySetId);
}
