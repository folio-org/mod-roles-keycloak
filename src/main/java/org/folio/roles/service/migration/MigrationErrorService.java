package org.folio.roles.service.migration;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.PermissionMigrationErrors;
import org.folio.roles.domain.entity.migration.PermissionMigrationErrorEntity;
import org.folio.roles.mapper.PermissionMigrationErrorMapper;
import org.folio.roles.repository.PermissionMigrationErrorRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MigrationErrorService {

  private final PermissionMigrationErrorRepository errorRepository;
  private final PermissionMigrationErrorMapper errorMapper;

  /**
   * Retrieves migration errors for a specific job.
   *
   * @param migrationJobId - migration job identifier
   * @param offset - offset for pagination
   * @param limit - limit for pagination
   * @return collection of migration errors
   */
  @Transactional(readOnly = true)
  public PermissionMigrationErrors getMigrationErrors(UUID migrationJobId, Integer offset, Integer limit) {
    var pageable = OffsetRequest.of(offset, limit);
    var errors = errorRepository.findByMigrationJobId(migrationJobId, pageable);
    return errorMapper.toDtoCollection(errors);
  }

  /**
   * Logs a migration error to the database.
   *
   * @param migrationJobId - migration job identifier
   * @param errorType - type of error
   * @param errorMessage - detailed error message
   * @param failedEntityType - type of entity that failed
   * @param failedEntityId - identifier of failed entity
   */
  @Transactional
  public void logError(UUID migrationJobId, String errorType, String errorMessage,
                       String failedEntityType, String failedEntityId) {
    var errorEntity = new PermissionMigrationErrorEntity();
    errorEntity.setId(UUID.randomUUID());
    errorEntity.setMigrationJobId(migrationJobId);
    errorEntity.setErrorType(errorType);
    errorEntity.setErrorMessage(truncateMessage(errorMessage));
    errorEntity.setFailedEntityType(failedEntityType);
    errorEntity.setFailedEntityId(failedEntityId);
    errorEntity.setOccurredAt(OffsetDateTime.now());

    errorRepository.save(errorEntity);
    log.debug("Migration error logged: jobId = {}, errorType = {}, entityType = {}, entityId = {}",
      migrationJobId, errorType, failedEntityType, failedEntityId);
  }

  private String truncateMessage(String message) {
    if (message == null) {
      return null;
    }
    return message.length() > 2000 ? message.substring(0, 2000) : message;
  }
}
