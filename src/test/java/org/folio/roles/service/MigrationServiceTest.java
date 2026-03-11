package org.folio.roles.service;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus.FAILED;
import static org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus.FINISHED;
import static org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus.IN_PROGRESS;
import static org.folio.roles.support.TestUtils.await;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.dto.PermissionMigrationJob;
import org.folio.roles.domain.dto.PermissionMigrationJobStatus;
import org.folio.roles.domain.dto.PermissionMigrationJobs;
import org.folio.roles.domain.entity.migration.PermissionMigrationJobEntity;
import org.folio.roles.domain.entity.type.EntityPermissionMigrationJobStatus;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.mapper.PermissionMigrationMapper;
import org.folio.roles.repository.PermissionMigrationJobRepository;
import org.folio.roles.service.migration.MigrationService;
import org.folio.roles.service.migration.PermissionMigrationService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

  private static final UUID MIGRATION_ID = UUID.randomUUID();

  @InjectMocks private MigrationService migrationService;
  @Mock private PermissionMigrationMapper permissionMigrationMapper;
  @Mock private PermissionMigrationService permissionMigrationService;
  @Mock private PermissionMigrationJobRepository migrationJobRepository;
  @Spy private final FolioExecutionContext folioExecutionContext =
    new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static PermissionMigrationJobEntity migrationJobEntity() {
    return migrationJobEntity(FINISHED);
  }

  private static PermissionMigrationJobEntity migrationJobEntity(EntityPermissionMigrationJobStatus status) {
    var entity = new PermissionMigrationJobEntity();
    entity.setId(MIGRATION_ID);
    entity.setStatus(status);
    entity.setStartedAt(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, UTC));
    entity.setFinishedAt(OffsetDateTime.of(2024, 1, 1, 1, 0, 0, 0, UTC));
    return entity;
  }

  private static PermissionMigrationJob migrationJob() {
    return migrationJob(PermissionMigrationJobStatus.FINISHED);
  }

  private static PermissionMigrationJob migrationJob(PermissionMigrationJobStatus status) {
    return new PermissionMigrationJob()
      .id(MIGRATION_ID)
      .status(status)
      .startedAt(Date.from(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, UTC).toInstant()))
      .finishedAt(Date.from(OffsetDateTime.of(2024, 1, 1, 1, 0, 0, 0, UTC).toInstant()));
  }

  @Nested
  @DisplayName("getMigrationById")
  class GetMigrationById {

    @Test
    void positive() {
      when(migrationJobRepository.getReferenceById(MIGRATION_ID)).thenReturn(migrationJobEntity());
      when(permissionMigrationMapper.toDto(migrationJobEntity())).thenReturn(migrationJob());

      var result = migrationService.getMigrationById(MIGRATION_ID);

      assertThat(result).isEqualTo(migrationJob());
    }

    @Test
    void negative_migrationNotFound() {
      when(migrationJobRepository.getReferenceById(MIGRATION_ID)).thenThrow(new EntityNotFoundException());
      assertThatThrownBy(() -> migrationService.getMigrationById(MIGRATION_ID))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findMigrations")
  class FindMigrations {

    @Test
    void positive() {
      var query = "cql.allRecords=1";
      var offsetRequest = OffsetRequest.of(0, 100);
      var entities = new PageImpl<>(List.of(migrationJobEntity()), offsetRequest, 1L);
      var expectedJobs = new PermissionMigrationJobs().totalRecords(1).migrations(List.of(migrationJob()));

      when(migrationJobRepository.findByCql(query, offsetRequest)).thenReturn(entities);
      when(permissionMigrationMapper.toDtoCollection(entities)).thenReturn(expectedJobs);

      var migrations = migrationService.findMigrations(query, 0, 100);

      assertThat(migrations).isEqualTo(expectedJobs);
    }

    @Test
    void positive_blankQuery() {
      var query = "  ";
      var offsetRequest = OffsetRequest.of(0, 100);
      var entities = new PageImpl<>(List.of(migrationJobEntity()), offsetRequest, 1L);
      var expectedJobs = new PermissionMigrationJobs().totalRecords(1).migrations(List.of(migrationJob()));

      when(migrationJobRepository.findAll(offsetRequest)).thenReturn(entities);
      when(permissionMigrationMapper.toDtoCollection(entities)).thenReturn(expectedJobs);

      var migrations = migrationService.findMigrations(query, 0, 100);

      assertThat(migrations).isEqualTo(expectedJobs);
    }
  }

  @Nested
  @DisplayName("deleteMigrationById")
  class DeleteMigrationById {

    @Test
    void positive() {
      when(migrationJobRepository.findById(MIGRATION_ID)).thenReturn(Optional.of(migrationJobEntity()));
      migrationService.deleteMigrationById(MIGRATION_ID);

      verify(migrationJobRepository).delete(migrationJobEntity());
    }

    @Test
    void positive_entityNotFound() {
      when(migrationJobRepository.findById(MIGRATION_ID)).thenReturn(Optional.empty());
      migrationService.deleteMigrationById(MIGRATION_ID);

      verify(migrationJobRepository, never()).delete(migrationJobEntity());
    }
  }

  @Nested
  @DisplayName("createMigration")
  class CreateMigration {

    @Captor private ArgumentCaptor<PermissionMigrationJobEntity> entityCaptor;

    @Test
    void positive() {
      when(migrationJobRepository.existsByStatus(IN_PROGRESS)).thenReturn(false);
      doNothing().when(migrationJobRepository).flush();
      doAnswer(answersWithDelay(100, inv -> 10)).when(permissionMigrationService).migratePermissions(MIGRATION_ID);
      var inProgressJob = migrationJob(PermissionMigrationJobStatus.IN_PROGRESS);
      var inProgressEntity = migrationJobEntity(IN_PROGRESS);

      when(migrationJobRepository.save(entityCaptor.capture())).thenAnswer(CreateMigration::entityWithNonGeneratedId);
      when(migrationJobRepository.findById(MIGRATION_ID)).thenReturn(Optional.of(inProgressEntity));
      when(permissionMigrationMapper.toDto(inProgressEntity)).thenReturn(inProgressJob);
      when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);

      try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
        var migration = migrationService.createMigration();
        assertThat(migration).isEqualTo(inProgressJob);
      }

      await().untilAsserted(() -> verify(migrationJobRepository).findById(MIGRATION_ID));
      verify(folioExecutionContext, atLeastOnce()).getTenantId();
      verify(folioExecutionContext, atLeastOnce()).getUserId();
      verify(folioExecutionContext, atLeastOnce()).getFolioModuleMetadata();
      verify(folioExecutionContext, atLeastOnce()).getRequestId();

      assertThat(entityCaptor.getAllValues().get(0).getStatus()).isEqualTo(IN_PROGRESS);
      assertThat(entityCaptor.getAllValues().get(0).getTotalRecords()).isNull();
      assertThat(entityCaptor.getAllValues().get(1).getStatus()).isEqualTo(FINISHED);
      assertThat(entityCaptor.getAllValues().get(1).getTotalRecords()).isEqualTo(10);
    }

    @Test
    void positive_entityNotFoundById() {
      when(migrationJobRepository.existsByStatus(IN_PROGRESS)).thenReturn(false);
      doNothing().when(migrationJobRepository).flush();
      doAnswer(answersWithDelay(100, inv -> 10)).when(permissionMigrationService).migratePermissions(MIGRATION_ID);
      var inProgressJob = migrationJob(PermissionMigrationJobStatus.IN_PROGRESS);
      var inProgressEntity = migrationJobEntity(IN_PROGRESS);

      when(migrationJobRepository.save(entityCaptor.capture())).thenAnswer(CreateMigration::entityWithNonGeneratedId);
      when(migrationJobRepository.findById(MIGRATION_ID)).thenReturn(Optional.empty());
      when(permissionMigrationMapper.toDto(inProgressEntity)).thenReturn(inProgressJob);
      when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);

      try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
        var migration = migrationService.createMigration();
        assertThat(migration).isEqualTo(inProgressJob);
      }

      await().untilAsserted(() -> verify(migrationJobRepository).findById(MIGRATION_ID));
      verify(folioExecutionContext, atLeastOnce()).getTenantId();
      verify(folioExecutionContext, atLeastOnce()).getUserId();
      verify(folioExecutionContext, atLeastOnce()).getFolioModuleMetadata();
      verify(folioExecutionContext, atLeastOnce()).getRequestId();

      assertThat(entityCaptor.getAllValues().get(0).getStatus()).isEqualTo(IN_PROGRESS);
      assertThat(entityCaptor.getAllValues().get(0).getTotalRecords()).isNull();
    }

    @Test
    void negative_migrationFailed() {
      when(migrationJobRepository.existsByStatus(IN_PROGRESS)).thenReturn(false);
      doNothing().when(migrationJobRepository).flush();
      doAnswer(answersWithDelay(100, throwMigrationException()))
        .when(permissionMigrationService).migratePermissions(MIGRATION_ID);

      var inProgressJob = migrationJob(PermissionMigrationJobStatus.IN_PROGRESS);
      var inProgressEntity = migrationJobEntity(IN_PROGRESS);

      when(migrationJobRepository.save(entityCaptor.capture())).thenAnswer(CreateMigration::entityWithNonGeneratedId);
      when(migrationJobRepository.findById(MIGRATION_ID)).thenReturn(Optional.of(inProgressEntity));
      when(permissionMigrationMapper.toDto(inProgressEntity)).thenReturn(inProgressJob);
      when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);

      try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
        var migration = migrationService.createMigration();
        assertThat(migration).isEqualTo(inProgressJob);
      }

      await().untilAsserted(() -> verify(migrationJobRepository).findById(MIGRATION_ID));
      verify(folioExecutionContext, atLeastOnce()).getTenantId();
      verify(folioExecutionContext, atLeastOnce()).getUserId();
      verify(folioExecutionContext, atLeastOnce()).getFolioModuleMetadata();
      verify(folioExecutionContext, atLeastOnce()).getRequestId();

      assertThat(entityCaptor.getAllValues().get(0).getStatus()).isEqualTo(IN_PROGRESS);
      assertThat(entityCaptor.getAllValues().get(0).getTotalRecords()).isNull();
      assertThat(entityCaptor.getAllValues().get(1).getStatus()).isEqualTo(FAILED);
      assertThat(entityCaptor.getAllValues().get(1).getTotalRecords()).isNull();
    }

    @Test
    void negative_concurrentMigrationInProgress() {
      when(migrationJobRepository.existsByStatus(IN_PROGRESS)).thenReturn(true);

      try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
        assertThatThrownBy(() -> migrationService.createMigration())
          .isInstanceOf(org.folio.roles.exception.RequestValidationException.class)
          .hasMessageContaining("There is already an active migration job in progress");
      }

      verify(folioExecutionContext, atLeastOnce()).getTenantId();
      verify(folioExecutionContext, atLeastOnce()).getUserId();
      verify(folioExecutionContext, atLeastOnce()).getFolioModuleMetadata();
      verify(folioExecutionContext, atLeastOnce()).getRequestId();

      verify(permissionMigrationService, never()).migratePermissions(org.mockito.ArgumentMatchers.any());
      verify(migrationJobRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static Answer<Object> throwMigrationException() {
      return inv -> {
        throw new MigrationException("error");
      };
    }

    private static PermissionMigrationJobEntity entityWithNonGeneratedId(InvocationOnMock inv) {
      var entity = inv.<PermissionMigrationJobEntity>getArgument(0);
      return migrationJobEntity(entity.getStatus());
    }
  }
}
