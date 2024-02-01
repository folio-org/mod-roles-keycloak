package org.folio.roles.repository;

import java.util.UUID;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadableRoleRepository extends BaseCqlJpaRepository<LoadableRoleEntity, UUID> {
}
