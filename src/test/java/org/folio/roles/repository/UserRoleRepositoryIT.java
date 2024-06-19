package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.Auditable;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.domain.entity.key.UserRoleKey;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRoleRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private UserRoleRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = Instancio.of(UserRoleEntity.class)
      .ignore(field(Auditable::getCreatedDate))
      .ignore(field(Auditable::getUpdatedDate))
      .create();
    var now = OffsetDateTime.now();

    repository.save(entity);

    var stored = entityManager.find(UserRoleEntity.class,
      UserRoleKey.of(entity.getUserId(), entity.getRoleId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }
}
