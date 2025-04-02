package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private CapabilitySetRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = capabilitySetEntity();
    entity.setId(null);
    var now = OffsetDateTime.now();

    var saved = repository.save(entity);

    var stored = entityManager.find(CapabilitySetEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findByCapabilityName_positive() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    var actualCapabilitySetEntities = repository.findByCapabilityName(capabilityEntity.getName());
    assertThat(actualCapabilitySetEntities).containsOnly(capabilitySetEntity);
  }

  @Test
  void addCapabilityById_positive() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity();
    capabilitySetEntity.setId(null);
    capabilitySetEntity.setCapabilities(null);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    repository.addCapabilityById(capabilitySetEntity.getId(), capabilityEntity.getId());

    var actualCapabilitySetEntities = repository.findByCapabilityName(capabilityEntity.getName());
    assertThat(actualCapabilitySetEntities).containsOnly(capabilitySetEntity);
  }
}
