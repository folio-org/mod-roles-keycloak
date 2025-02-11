package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private CapabilitySetRepository capabilitySetRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = capabilitySetEntity();
    entity.setId(null);
    var now = OffsetDateTime.now();

    var saved = capabilitySetRepository.save(entity);

    var stored = entityManager.find(CapabilitySetEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void existsByName_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);

    assertThat(capabilitySetRepository.existsByName(capabilitySetEntity.getName())).isTrue();
    assertThat(capabilitySetRepository.existsByName(dummyCapabilitySetEntity.getName())).isFalse();
  }

  @Test
  void findByUserId_positive_includeAndExcludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var userId = UUID.randomUUID();
    var userCapabilitySetEntity = userCapabilitySetEntity(userId, capabilitySetEntity.getId());
    var userDummyCapabilitySetEntity = userCapabilitySetEntity(userId, dummyCapabilitySetEntity.getId());
    entityManager.persistAndFlush(userCapabilitySetEntity);
    entityManager.persistAndFlush(userDummyCapabilitySetEntity);

    var offsetRequest = OffsetRequest.of(0, 1, CapabilitySetEntity.DEFAULT_CAPABILITY_SET_SORT);
    var page = capabilitySetRepository.findByUserId(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.stream().noneMatch(CapabilitySetEntity::isDummyCapability)).isTrue();

    page = capabilitySetRepository.findByUserIdIncludeDummy(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findByRoleId_positive_includeAndExcludeDummy() {
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(roleEntity);
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var roleCapabilitySet = roleCapabilitySetEntity(roleId, capabilitySetEntity.getId());
    var roleDummyCapabilitySet = roleCapabilitySetEntity(roleId, dummyCapabilitySetEntity.getId());
    entityManager.persistAndFlush(roleCapabilitySet);
    entityManager.persistAndFlush(roleDummyCapabilitySet);

    var offsetRequest = OffsetRequest.of(0, 1, CapabilitySetEntity.DEFAULT_CAPABILITY_SET_SORT);
    var page = capabilitySetRepository.findByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.stream().noneMatch(CapabilitySetEntity::isDummyCapability)).isTrue();

    page = capabilitySetRepository.findByRoleIdIncludeDummy(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findCapabilitySetIdsByIdIn_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var capabilitySetIds = List.of(capabilitySetEntity.getId(), dummyCapabilitySetEntity.getId());

    var actualCapabilitySetIds  = capabilitySetRepository.findCapabilitySetIdsByIdIn(capabilitySetIds);
    assertThat(actualCapabilitySetIds).hasSize(1).contains(capabilitySetEntity.getId());
  }

  @Test
  void findByName_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, List.of());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, List.of());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);

    var actual = capabilitySetRepository.findByName(dummyCapabilitySetEntity.getName());
    assertThat(actual).isEmpty();

    actual = capabilitySetRepository.findByName(capabilitySetEntity.getName());
    assertThat(actual).isNotEmpty().get().isEqualTo(capabilitySetEntity);
  }

  @Test
  void findByNameIn_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);
    var capabilitySetNames = List.of(capabilitySetEntity.getName(), capabilitySetEntity.getName());

    var actual = capabilitySetRepository.findByNameIn(capabilitySetNames);
    assertThat(actual).hasSize(1).contains(capabilitySetEntity);
  }

  @Test
  void findByPermissionNames_positive_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    capabilitySetEntity.setPermission("permission_for_capabilitySet");
    var dummyCapabilitySetEntity = capabilitySetEntity(null, emptyList());
    dummyCapabilitySetEntity.setDummyCapability(true);
    dummyCapabilitySetEntity.setName("dummy_" + UUID.randomUUID());
    dummyCapabilitySetEntity.setPermission("permission_for_dummyCapabilitySet");
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(dummyCapabilitySetEntity);

    var permissionNames = List.of("permission_for_capabilitySet", "permission_for_dummyCapabilitySet");
    var actual = capabilitySetRepository.findByPermissionNames(permissionNames);
    assertThat(actual).hasSize(1).contains(capabilitySetEntity);
  }
}
