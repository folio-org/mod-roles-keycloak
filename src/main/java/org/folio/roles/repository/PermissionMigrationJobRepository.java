package org.folio.roles.repository;

import java.util.UUID;
import org.folio.roles.domain.entity.migration.PermissionMigrationJobEntity;
import org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionMigrationJobRepository extends JpaCqlRepository<PermissionMigrationJobEntity, UUID> {

  boolean existsByStatus(EntityPermissionMigrationJobStatus status);
}
