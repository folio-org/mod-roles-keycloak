package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.key.RoleCapabilityKey;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleCapabilityRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private RoleCapabilityRepository roleCapabilityRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = roleCapabilityEntity();
    var now = OffsetDateTime.now();

    roleCapabilityRepository.save(entity);

    var stored = entityManager.find(RoleCapabilityEntity.class,
      RoleCapabilityKey.of(entity.getRoleId(), entity.getCapabilityId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findAllByRoleId_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummyCapability");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilityEntity = entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));

    var roleCapabilityEntities = roleCapabilityRepository.findAllByRoleId(roleId);
    assertThat(roleCapabilityEntities).hasSize(1).contains(roleCapabilityEntity);
  }

  @Test
  void findAllByCapabilityId_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummyCapability");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilityEntity = entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));

    var roleCapabilityEntities = roleCapabilityRepository.findAllByCapabilityId(capabilityEntity.getId());
    assertThat(roleCapabilityEntities).hasSize(1).contains(roleCapabilityEntity);
    roleCapabilityEntities = roleCapabilityRepository.findAllByCapabilityId(dummyCapabilityEntity.getId());
    assertThat(roleCapabilityEntities).isEmpty();
  }

  @Test
  void findByRoleId_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummyCapability");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilityEntity = entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));

    var offsetRequest = OffsetRequest.of(0, 2, RoleCapabilityEntity.DEFAULT_ROLE_CAPABILITY_SORT);
    var page = roleCapabilityRepository.findByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0)).isEqualTo(roleCapabilityEntity);
  }

  @Test
  void findRoleCapabilities_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummyCapability");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilityEntity = entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));

    var roleCapabilityEntities = roleCapabilityRepository.findRoleCapabilities(roleId,
      List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId()));
    assertThat(roleCapabilityEntities).hasSize(1).contains(roleCapabilityEntity);
  }
}
