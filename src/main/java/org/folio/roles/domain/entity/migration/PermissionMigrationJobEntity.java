package org.folio.roles.domain.entity.migration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;
import org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "permission_migration_job")
public class PermissionMigrationJobEntity {

  /**
   * An entity identifier.
   */
  @Id
  private UUID id;

  /**
   * Total number of records to be migrated.
   */
  @Column(name = "total_records")
  private Integer totalRecords;

  /**
   * A permission migration job status.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", columnDefinition = "permission_migration_job_status_type")
  private EntityPermissionMigrationJobStatus status;

  /**
   * A permission migration job startup timestamp.
   */
  @Column(name = "started_at", nullable = false, updatable = false)
  private OffsetDateTime startedAt;

  /**
   * A permission migration job finishing timestamp.
   */
  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;
}
