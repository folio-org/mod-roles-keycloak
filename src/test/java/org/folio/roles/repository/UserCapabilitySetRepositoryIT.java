package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.key.UserCapabilitySetKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserCapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private UserCapabilitySetRepository userCapabilitySetRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = userCapabilitySetEntity();
    var now = OffsetDateTime.now();

    userCapabilitySetRepository.save(entity);

    var stored = entityManager.find(UserCapabilitySetEntity.class,
      UserCapabilitySetKey.of(entity.getUserId(), entity.getCapabilitySetId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findAllByUserId_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var userId = UUID.randomUUID();
    var userCapabilitySetEntity = entityManager
      .persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, dummyCapabilitySetEntity.getId()));

    var userCapabilitySets = userCapabilitySetRepository.findAllByUserId(userId);
    assertThat(userCapabilitySets).containsOnly(userCapabilitySetEntity);
  }

  @Test
  void findAllByCapabilitySetId_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var userId = UUID.randomUUID();
    var userCapabilitySetEntity = entityManager
      .persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, dummyCapabilitySetEntity.getId()));

    var userCapabilitySets = userCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetEntity.getId());
    assertThat(userCapabilitySets).containsOnly(userCapabilitySetEntity);
    userCapabilitySets = userCapabilitySetRepository.findAllByCapabilitySetId(dummyCapabilitySetEntity.getId());
    assertThat(userCapabilitySets).isEmpty();
  }

  @Test
  void findUserCapabilitySets_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var userId = UUID.randomUUID();
    var userCapabilitySetEntity = entityManager
      .persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, dummyCapabilitySetEntity.getId()));

    var userCapabilitySets = userCapabilitySetRepository.findUserCapabilitySets(userId,
      List.of(capabilitySetEntity.getId(), dummyCapabilitySetEntity.getId()));
    assertThat(userCapabilitySets).containsOnly(userCapabilitySetEntity);
  }
}
