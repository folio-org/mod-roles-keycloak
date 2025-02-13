package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilityEntity;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.key.UserCapabilityKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserCapabilityRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private UserCapabilityRepository userCapabilityRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = userCapabilityEntity();
    var now = OffsetDateTime.now();

    userCapabilityRepository.save(entity);

    var stored = entityManager
      .find(UserCapabilityEntity.class, UserCapabilityKey.of(entity.getUserId(), entity.getCapabilityId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findAllByUserId_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    var userCapabilityEntity = entityManager
      .persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));

    var userCapabilities = userCapabilityRepository.findAllByUserId(userId);
    assertThat(userCapabilities).containsOnly(userCapabilityEntity);
  }

  @Test
  void findAllByCapabilityId_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    var userCapabilityEntity = entityManager
      .persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));

    var userCapabilities = userCapabilityRepository.findAllByCapabilityId(capabilityEntity.getId());
    assertThat(userCapabilities).containsOnly(userCapabilityEntity);
    userCapabilities = userCapabilityRepository.findAllByCapabilityId(dummyCapabilityEntity.getId());
    assertThat(userCapabilities).isEmpty();
  }

  @Test
  void findUserCapabilities_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    var userCapabilityEntity = entityManager
      .persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));

    var userCapabilities = userCapabilityRepository.findUserCapabilities(userId,
        List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId()));
    assertThat(userCapabilities).containsOnly(userCapabilityEntity);
  }
}
