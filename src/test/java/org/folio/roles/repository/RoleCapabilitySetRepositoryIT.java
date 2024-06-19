package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleCapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private RoleCapabilitySetRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = roleCapabilitySetEntity();
    var now = OffsetDateTime.now();

    repository.save(entity);

    var stored = entityManager.find(RoleCapabilitySetEntity.class,
      RoleCapabilitySetKey.of(entity.getRoleId(), entity.getCapabilitySetId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }
}
