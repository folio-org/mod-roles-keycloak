package org.folio.roles.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadableRoleRepository extends BaseCqlJpaRepository<LoadableRoleEntity, UUID> {

  Optional<LoadableRoleEntity> findByIdOrName(UUID id, String name);

  // TODO (Dima Tkachenko): review code
  List<LoadableRoleEntity> findAllByPermissions(Set<String> permissionNames);
}
