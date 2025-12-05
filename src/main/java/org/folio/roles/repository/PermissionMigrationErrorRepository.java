package org.folio.roles.repository;

import java.util.UUID;
import org.folio.roles.domain.entity.migration.PermissionMigrationErrorEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionMigrationErrorRepository extends JpaCqlRepository<PermissionMigrationErrorEntity, UUID> {

  /**
   * Finds migration errors by migration job ID.
   *
   * @param migrationJobId - migration job identifier
   * @param pageable - pagination information
   * @return page of migration errors
   */
  Page<PermissionMigrationErrorEntity> findByMigrationJobId(UUID migrationJobId, Pageable pageable);
}
