package org.folio.roles.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadableRoleRepository extends BaseCqlJpaRepository<LoadableRoleEntity, UUID> {

  Optional<LoadableRoleEntity> findByIdOrName(UUID id, String name);

  @Query("""
    select distinct r from LoadableRoleEntity r left join fetch r.permissions
      where r.id = :id or r.name = :name""")
  Optional<LoadableRoleEntity> findByIdOrNameWithPermissions(@Param("id") UUID id, @Param("name") String name);

  boolean existsByIdAndType(UUID id, EntityRoleType type);

  Stream<LoadableRoleEntity> findAllByType(EntityRoleType type);

  Stream<LoadableRoleEntity> findAllByTypeAndLoadedFromFile(EntityRoleType type, boolean loadedFromFile);
}
