package org.folio.roles.domain.entity;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
   * An user migration job status.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", columnDefinition = "permission_migration_job_status_type")
  private EntityPermissionMigrationJobStatus status;

  /**
   * An user migration job startup timestamp.
   */
  @CreatedDate
  @Column(name = "started_at", nullable = false, updatable = false)
  private OffsetDateTime startedAt;

  /**
   * A user migration job finishing timestamp.
   */
  @LastModifiedDate
  @Column(name = "finished_at", nullable = false)
  private OffsetDateTime finishedAt;
}
