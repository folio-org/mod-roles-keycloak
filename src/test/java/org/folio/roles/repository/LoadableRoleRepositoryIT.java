package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.LoadableRoleUtils.loadableRole;
import static org.folio.roles.support.LoadableRoleUtils.loadableRoleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoadableRoleRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private LoadableRoleRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updateDateAndCreatedDateNotNull() {
    var role = loadableRole();
    role.setMetadata(null);
    role.setPermissions(List.of());

    var entity = loadableRoleEntity(role);
    var now = OffsetDateTime.now();

    var saved = repository.save(entity);

    var stored = entityManager.find(LoadableRoleEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
  }
}
