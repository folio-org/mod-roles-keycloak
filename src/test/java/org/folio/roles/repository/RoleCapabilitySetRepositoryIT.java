package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.key.RoleCapabilitySetKey;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleCapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private RoleCapabilitySetRepository roleCapabilitySetRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = roleCapabilitySetEntity();
    var now = OffsetDateTime.now();

    roleCapabilitySetRepository.save(entity);

    var stored = entityManager.find(RoleCapabilitySetEntity.class,
      RoleCapabilitySetKey.of(entity.getRoleId(), entity.getCapabilitySetId()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findAllByRoleId_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummyCapabilitySet");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilitySetEntity = entityManager.persistAndFlush(roleCapabilitySetEntity(roleId,
      capabilitySetEntity.getId()));
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, dummyCapabilitySetEntity.getId()));

    var roleCapabilityEntities = roleCapabilitySetRepository.findAllByRoleId(roleId);
    assertThat(roleCapabilityEntities).hasSize(1).contains(roleCapabilitySetEntity);
  }

  @Test
  void findAllByCapabilitySetId_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummyCapabilitySet");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilitySetEntity = entityManager.persistAndFlush(roleCapabilitySetEntity(roleId,
      capabilitySetEntity.getId()));
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, dummyCapabilitySetEntity.getId()));

    var roleCapabilityEntities = roleCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetEntity.getId());
    assertThat(roleCapabilityEntities).hasSize(1).contains(roleCapabilitySetEntity);
    roleCapabilityEntities = roleCapabilitySetRepository.findAllByCapabilitySetId(dummyCapabilitySetEntity.getId());
    assertThat(roleCapabilityEntities).isEmpty();
  }

  @Test
  void findRoleCapabilitySets_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummyCapabilitySet");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilitySetEntity = entityManager.persistAndFlush(roleCapabilitySetEntity(roleId,
      capabilitySetEntity.getId()));
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, dummyCapabilitySetEntity.getId()));

    var roleCapabilitySetEntities = roleCapabilitySetRepository.findRoleCapabilitySets(roleId,
      List.of(capabilitySetEntity.getId(), dummyCapabilitySetEntity.getId()));
    assertThat(roleCapabilitySetEntities).hasSize(1).contains(roleCapabilitySetEntity);
  }

  @Test
  void findByRoleId_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummyCapabilitySet");
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    entityManager.persistAndFlush(roleEntity);
    var roleCapabilitySetEntity = entityManager.persistAndFlush(roleCapabilitySetEntity(roleId,
      capabilitySetEntity.getId()));
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, dummyCapabilitySetEntity.getId()));

    var offsetRequest = OffsetRequest.of(0, 2, RoleCapabilitySetEntity.DEFAULT_ROLE_CAPABILITY_SET_SORT);
    var page = roleCapabilitySetRepository.findByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0)).isEqualTo(roleCapabilitySetEntity);
  }
}
