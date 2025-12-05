package org.folio.roles.service.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.folio.roles.domain.dto.PermissionMigrationError;
import org.folio.roles.domain.dto.PermissionMigrationErrors;
import org.folio.roles.domain.entity.migration.PermissionMigrationErrorEntity;
import org.folio.roles.mapper.PermissionMigrationErrorMapper;
import org.folio.roles.repository.PermissionMigrationErrorRepository;
import org.folio.roles.support.TestUtils;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MigrationErrorServiceTest {

  private static final UUID MIGRATION_JOB_ID = UUID.randomUUID();

  @InjectMocks private MigrationErrorService service;

  @Mock private PermissionMigrationErrorRepository errorRepository;
  @Mock private PermissionMigrationErrorMapper errorMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void getMigrationErrors_positive() {
    var offset = 0;
    var limit = 10;
    var errorEntity = permissionMigrationErrorEntity();
    var page = new PageImpl<>(java.util.List.of(errorEntity));
    var expectedErrors = new PermissionMigrationErrors()
      .errors(java.util.List.of(new PermissionMigrationError()))
      .totalRecords(1);

    when(errorRepository.findByMigrationJobId(MIGRATION_JOB_ID, OffsetRequest.of(offset, limit)))
      .thenReturn(page);
    when(errorMapper.toDtoCollection(page)).thenReturn(expectedErrors);

    var result = service.getMigrationErrors(MIGRATION_JOB_ID, offset, limit);

    assertThat(result).isEqualTo(expectedErrors);
    verify(errorRepository).findByMigrationJobId(MIGRATION_JOB_ID, OffsetRequest.of(offset, limit));
    verify(errorMapper).toDtoCollection(page);
  }

  @Test
  void getMigrationErrors_positive_withDifferentPagination() {
    var offset = 20;
    var limit = 50;
    var page = new PageImpl<PermissionMigrationErrorEntity>(java.util.List.of());
    var expectedErrors = new PermissionMigrationErrors()
      .errors(java.util.List.of())
      .totalRecords(0);

    when(errorRepository.findByMigrationJobId(MIGRATION_JOB_ID, OffsetRequest.of(offset, limit)))
      .thenReturn(page);
    when(errorMapper.toDtoCollection(page)).thenReturn(expectedErrors);

    var result = service.getMigrationErrors(MIGRATION_JOB_ID, offset, limit);

    assertThat(result).isEqualTo(expectedErrors);
    verify(errorRepository).findByMigrationJobId(MIGRATION_JOB_ID, OffsetRequest.of(offset, limit));
    verify(errorMapper).toDtoCollection(page);
  }

  @Test
  void logError_positive() {
    var errorType = "ROLE_CREATION_FAILED";
    var errorMessage = "Failed to create role in Keycloak";
    var entityType = "Role";
    var entityId = "test-role-id";

    service.logError(MIGRATION_JOB_ID, errorType, errorMessage, entityType, entityId);

    var captor = ArgumentCaptor.forClass(PermissionMigrationErrorEntity.class);
    verify(errorRepository).save(captor.capture());

    var savedEntity = captor.getValue();
    assertThat(savedEntity.getId()).isNotNull();
    assertThat(savedEntity.getMigrationJobId()).isEqualTo(MIGRATION_JOB_ID);
    assertThat(savedEntity.getErrorType()).isEqualTo(errorType);
    assertThat(savedEntity.getErrorMessage()).isEqualTo(errorMessage);
    assertThat(savedEntity.getFailedEntityType()).isEqualTo(entityType);
    assertThat(savedEntity.getFailedEntityId()).isEqualTo(entityId);
    assertThat(savedEntity.getOccurredAt()).isNotNull();
  }

  @Test
  void logError_positive_truncatesLongMessage() {
    var longMessage = "a".repeat(3000);
    var errorType = "TEST_ERROR";
    var entityType = "TestEntity";
    var entityId = "test-id";

    service.logError(MIGRATION_JOB_ID, errorType, longMessage, entityType, entityId);

    var captor = ArgumentCaptor.forClass(PermissionMigrationErrorEntity.class);
    verify(errorRepository).save(captor.capture());

    var savedEntity = captor.getValue();
    assertThat(savedEntity.getErrorMessage()).hasSize(2000);
    assertThat(savedEntity.getErrorMessage()).isEqualTo(longMessage.substring(0, 2000));
  }

  @Test
  void logError_positive_handlesNullMessage() {
    var errorType = "NULL_MESSAGE_ERROR";
    var entityType = "TestEntity";
    var entityId = "test-id";

    service.logError(MIGRATION_JOB_ID, errorType, null, entityType, entityId);

    var captor = ArgumentCaptor.forClass(PermissionMigrationErrorEntity.class);
    verify(errorRepository).save(captor.capture());

    var savedEntity = captor.getValue();
    assertThat(savedEntity.getErrorMessage()).isNull();
  }

  @Test
  void logError_positive_handlesEmptyMessage() {
    var errorType = "EMPTY_MESSAGE_ERROR";
    var errorMessage = "";
    var entityType = "TestEntity";
    var entityId = "test-id";

    service.logError(MIGRATION_JOB_ID, errorType, errorMessage, entityType, entityId);

    var captor = ArgumentCaptor.forClass(PermissionMigrationErrorEntity.class);
    verify(errorRepository).save(captor.capture());

    var savedEntity = captor.getValue();
    assertThat(savedEntity.getErrorMessage()).isEmpty();
  }

  private static PermissionMigrationErrorEntity permissionMigrationErrorEntity() {
    var entity = new PermissionMigrationErrorEntity();
    entity.setId(UUID.randomUUID());
    entity.setMigrationJobId(MIGRATION_JOB_ID);
    entity.setErrorType("TEST_ERROR");
    entity.setErrorMessage("Test error message");
    entity.setFailedEntityType("Role");
    entity.setFailedEntityId("test-role");
    entity.setOccurredAt(OffsetDateTime.now());
    return entity;
  }
}
