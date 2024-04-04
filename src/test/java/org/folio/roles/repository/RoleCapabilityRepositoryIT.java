package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleCapabilityRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private RoleCapabilityRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updateDateAndCreatedDateNotNull() {
    var entity = roleCapabilityEntity();
    var now = OffsetDateTime.now();

    repository.save(entity);

    var stored = entityManager.find(RoleCapabilityEntity.class,
      RoleCapabilityKey.of(entity.getRoleId(), entity.getCapabilityId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
  }
}
