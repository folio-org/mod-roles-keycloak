package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "permission_migration_error")
public class PermissionMigrationErrorEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "migration_job_id")
  private UUID migrationJobId;

  @Column(name = "error_type")
  private String errorType;

  @Column(name = "error_message", length = 2000)
  private String errorMessage;

  @Column(name = "failed_entity_type")
  private String failedEntityType;

  @Column(name = "failed_entity_id")
  private String failedEntityId;

  @Column(name = "occurred_at")
  private OffsetDateTime occurredAt;
}
