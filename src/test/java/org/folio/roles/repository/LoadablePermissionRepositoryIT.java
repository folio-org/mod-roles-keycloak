package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoadablePermissionRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private LoadablePermissionRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var perm = loadablePermission();
    perm.setMetadata(null);

    var roleId = UUID.randomUUID();
    var entity = loadablePermissionEntity(roleId, perm);
    var now = OffsetDateTime.now();

    repository.save(entity);

    var stored = entityManager.find(LoadablePermissionEntity.class, LoadablePermissionKey.of(roleId,
      perm.getPermissionName()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedBy()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedBy()).isEqualTo(USER_ID);
  }
}
