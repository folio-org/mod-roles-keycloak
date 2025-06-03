package org.folio.roles.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadableRoleRepository extends BaseCqlJpaRepository<LoadableRoleEntity, UUID> {

  Optional<LoadableRoleEntity> findByIdOrName(UUID id, String name);

  boolean existsByIdAndType(UUID id, EntityRoleType type);

  Stream<LoadableRoleEntity> findAllByType(EntityRoleType type);

  Stream<LoadableRoleEntity> findAllByTypeAndLoadedFromFile(EntityRoleType type, boolean loadedFromFile);
}
