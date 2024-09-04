package org.folio.roles.service;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus.IN_PROGRESS;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.PermissionMigrationJobs;
import org.folio.roles.domain.entity.PermissionMigrationJobEntity;
import org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus;
import org.folio.roles.mapper.PermissionMigrationMapper;
import org.folio.roles.repository.PermissionMigrationJobRepository;
import org.folio.roles.service.migration.PermissionMigrationService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MigrationService {

  private final FolioExecutionContext folioExecutionContext;
  private final PermissionMigrationMapper migrationJobMapper;
  private final PermissionMigrationService permissionMigrationService;
  private final PermissionMigrationJobRepository migrationJobRepository;

  private final ExecutorService executor = getMigrationJobExecutor();

  /**
   * Retrieves permission migration by id.
   *
   * @param id - permission migration identifier
   * @return permission migration job by id
   * @throws EntityNotFoundException when migration job is not found by id
   */
  @Transactional(readOnly = true)
  public PermissionMigrationJob getMigrationById(UUID id) {
    var entity = migrationJobRepository.getReferenceById(id);
    return migrationJobMapper.toDto(entity);
  }

  /**
   * Retrieves permission migration by query.
   *
   * @param query - CQL query string
   * @param offset - offset in pagination from first record
   * @param limit - a number of results in response
   * @return permission migration job by id
   * @throws EntityNotFoundException when migration job is not found by id
   */
  @Transactional(readOnly = true)
  public PermissionMigrationJobs findMigrations(String query, Integer offset, Integer limit) {
    var offsetReq = OffsetRequest.of(offset, limit);

    var page = StringUtils.isBlank(query)
      ? migrationJobRepository.findAll(offsetReq)
      : migrationJobRepository.findByCql(query, offsetReq);

    return new PermissionMigrationJobs()
      .migrations(mapItems(page.getContent(), migrationJobMapper::toDto))
      .totalRecords((int) page.getTotalElements());
  }

  /**
   * Removes permission migration by id if exists.
   *
   * @param id - permission migration identifier
   */
  @Transactional
  public void deleteMigrationById(UUID id) {
    migrationJobRepository.findById(id).ifPresent(migrationJobRepository::delete);
  }

  /**
   * Creates permission migration job.
   *
   * @return {@link PermissionMigrationJob} object
   */
  @Transactional
  public PermissionMigrationJob createMigration() {
    var migrationEntity = buildPermissionMigrationsEntity();
    var savedEntity = migrationJobRepository.save(migrationEntity);
    migrationJobRepository.flush();

    startMigration(savedEntity);

    return migrationJobMapper.toDto(savedEntity);
  }

  private void startMigration(PermissionMigrationJobEntity jobEntity) {
    var migrationJob = (Runnable) () -> permissionMigrationService.migratePermissions(jobEntity.getId());
    runAsync(getRunnableWithCurrentFolioContext(migrationJob), executor)
      .whenComplete(handleMigrationComplete(jobEntity, (FolioExecutionContext) folioExecutionContext.getInstance()));
  }

  private BiConsumer<Void, ? super Throwable> handleMigrationComplete(
    PermissionMigrationJobEntity job, FolioExecutionContext ctx) {
    return (unused, error) -> {
      try (var ignored = new FolioExecutionContextSetter(ctx)) {
        var status = getMigrationStatus(job, error);

        migrationJobRepository.findById(job.getId())
          .ifPresent(entity -> {
            entity.setStatus(status);
            entity.setFinishedAt(OffsetDateTime.now());
            migrationJobRepository.save(entity);
          });
      }
    };
  }

  private static EntityPermissionMigrationJobStatus getMigrationStatus(PermissionMigrationJobEntity job, Throwable ex) {
    if (ex != null) {
      log.warn("Permission migration was failed: jobId = {}", job.getId(), ex);
      return EntityPermissionMigrationJobStatus.FAILED;
    }

    return EntityPermissionMigrationJobStatus.FINISHED;
  }

  private static PermissionMigrationJobEntity buildPermissionMigrationsEntity() {
    var migration = new PermissionMigrationJobEntity();
    migration.setId(UUID.randomUUID());
    migration.setStatus(IN_PROGRESS);
    migration.setStartedAt(OffsetDateTime.now());
    return migration;
  }

  private static ExecutorService getMigrationJobExecutor() {
    var corePoolSize = 1;
    var maxPoolSize = 2;

    var executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    executor.allowCoreThreadTimeOut(true);

    return executor;
  }
}
