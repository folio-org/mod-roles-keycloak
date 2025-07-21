package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilityEntity;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapabilityRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private CapabilityRepository capabilityRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = capabilityEntity();
    entity.setId(null);
    var now = OffsetDateTime.now();

    var saved = capabilityRepository.save(entity);

    var stored = entityManager.find(CapabilityEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.isDummyCapability()).isFalse();
  }

  @Test
  void findByCapabilitySetId_positive_excludeAndIncludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    dummyCapabilityEntity = entityManager.persistAndFlush(dummyCapabilityEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId(),
      dummyCapabilityEntity.getId()));
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    var offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var page = capabilityRepository.findByCapabilitySetId(capabilitySetEntity.getId(), offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    page = capabilityRepository.findByCapabilitySetId(capabilitySetEntity.getId(), offsetRequest);
    assertThat(page.getTotalElements()).isZero();

    page = capabilityRepository.findByCapabilitySetIdIncludeDummy(capabilitySetEntity.getId(), offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findByUserId_positive_excludeAndIncludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));

    var offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var page = capabilityRepository.findByUserId(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();

    page = capabilityRepository.findByUserIdIncludeDummy(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findAllByUserId_positive_excludeAndIncludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));
    capabilityEntity = capabilityEntity(null);
    capabilityEntity.setName(capabilityEntity.getName() + "_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);
    var userCapabilitySetEntity = userCapabilitySetEntity(userId, capabilitySetEntity.getId());
    entityManager.persistAndFlush(userCapabilitySetEntity);

    var offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var page = capabilityRepository.findAllByUserId(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();
    offsetRequest = OffsetRequest.of(1, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    page = capabilityRepository.findAllByUserId(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();

    offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    page = capabilityRepository.findAllByUserIdIncludeDummy(userId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(3);
  }

  @Test
  void findByRoleId_positive_excludeAndIncludeDummy() {
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));

    var offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var page = capabilityRepository.findByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();

    page = capabilityRepository.findByRoleIdIncludeDummy(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findAllByRoleId_positive_excludeAndIncludeDummy() {
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    dummyCapabilityEntity = entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, dummyCapabilityEntity.getId()));
    var capabilityCapabilitySetEntity = capabilityEntity(null);
    capabilityCapabilitySetEntity.setName(capabilityEntity.getName() + "_" + capabilityEntity.getId());
    capabilityCapabilitySetEntity = entityManager.persistAndFlush(capabilityCapabilitySetEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityCapabilitySetEntity.getId()));
    entityManager.persistAndFlush(capabilitySetEntity);
    var roleCapabilitySet = roleCapabilitySetEntity(roleId, capabilitySetEntity.getId());
    entityManager.persistAndFlush(roleCapabilitySet);

    var offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    var page = capabilityRepository.findAllByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();
    offsetRequest = OffsetRequest.of(1, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    page = capabilityRepository.findAllByRoleId(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.stream().noneMatch(CapabilityEntity::isDummyCapability)).isTrue();

    offsetRequest = OffsetRequest.of(0, 1, CapabilityEntity.DEFAULT_CAPABILITY_SORT);
    page = capabilityRepository.findAllByRoleIdIncludeDummy(roleId, offsetRequest);
    assertThat(page.getTotalElements()).isEqualTo(3);
  }

  @Test
  void findAllByNames_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);

    var capabilityEntities = capabilityRepository.findAllByNames(List.of(capabilityEntity.getName(),
      dummyCapabilityEntity.getName()));
    assertThat(capabilityEntities).hasSize(1);
    var actualCapability = capabilityEntities.get(0);
    assertThat(actualCapability.getName()).isEqualTo(capabilityEntity.getName());
  }

  @Test
  void findAllByNamesIncludeDummy_positive_includeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);

    var capabilityEntities = capabilityRepository.findAllByNamesIncludeDummy(List.of(capabilityEntity.getName(),
      dummyCapabilityEntity.getName()));
    assertThat(capabilityEntities).hasSize(2);
  }

  @Test
  void findByName_positive_excludeDummy() {
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(dummyCapabilityEntity);

    var capabilityEntity = capabilityRepository.findByName(dummyCapabilityEntity.getName());
    assertThat(capabilityEntity).isEmpty();
  }

  @Test
  void findCapabilityIdsByIdIn_positive_includeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    dummyCapabilityEntity = entityManager.persistAndFlush(dummyCapabilityEntity);

    var capabilityEntities = capabilityRepository.findCapabilityIdsByIdIncludeDummy(List.of(capabilityEntity.getId(),
      dummyCapabilityEntity.getId()));
    assertThat(capabilityEntities).hasSize(2);
  }

  @Test
  void findByCapabilitySetIds_positive_excludeDummyAndIncludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    var capabilitiesIds = List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId());
    var capabilitySetEntity = capabilitySetEntity(null, capabilitiesIds);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    var capabilityEntities = capabilityRepository.findByCapabilitySetIds(List.of(capabilitySetEntity.getId()));
    assertThat(capabilityEntities).hasSize(1);
    capabilityEntities = capabilityRepository.findByCapabilitySetIdsIncludeDummy(List.of(capabilitySetEntity.getId()));
    assertThat(capabilityEntities).hasSize(2);
  }

  @Test
  void findPermissionsByPrefixes_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity.setPermission("permission-not-for-dummy");
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    dummyCapabilityEntity.setPermission("permission-for-dummy");
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));
    var capabilitiesIds = List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId());
    var capabilitySetEntity = capabilitySetEntity(null, capabilitiesIds);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);
    var userCapabilitySetEntity = userCapabilitySetEntity(userId, capabilitySetEntity.getId());
    entityManager.persistAndFlush(userCapabilitySetEntity);

    var permissions = capabilityRepository.findPermissionsByPrefixes(userId, "{permission}");
    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0)).isEqualTo("permission-not-for-dummy");
  }

  @Test
  void findAllFolioPermissions_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity.setPermission("permission-not-for-dummy");
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    dummyCapabilityEntity.setPermission("permission-for-dummy");
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));
    var capabilitiesIds = List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId());
    var capabilitySetEntity = capabilitySetEntity(null, capabilitiesIds);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);
    var userCapabilitySetEntity = userCapabilitySetEntity(userId, capabilitySetEntity.getId());
    entityManager.persistAndFlush(userCapabilitySetEntity);

    var permissions = capabilityRepository.findAllFolioPermissions(userId);
    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0)).isEqualTo("permission-not-for-dummy");
  }

  @Test
  void findAllByPermissionName_positive_excludeDummyAndIncludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity.setPermission("permission-not-for-dummy");
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    dummyCapabilityEntity.setPermission("permission-for-dummy");
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);

    var capabilityEntities = capabilityRepository
      .findAllByPermissionNames(List.of("permission-not-for-dummy", "permission-for-dummy"));
    assertThat(capabilityEntities).hasSize(1);
    assertThat(capabilityEntities.get(0).isDummyCapability()).isFalse();

    capabilityEntities = capabilityRepository
      .findAllByPermissionNamesIncludeDummy(List.of("permission-not-for-dummy", "permission-for-dummy"));
    assertThat(capabilityEntities).hasSize(2);
  }

  @Test
  void findPermissionsByPrefixesAndPermissionNames_positive_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity.setPermission("permission-not-for-dummy");
    var dummyCapabilityEntity = capabilityEntity(null);
    dummyCapabilityEntity.setDummyCapability(true);
    dummyCapabilityEntity.setName("dummy_" + UUID.randomUUID());
    dummyCapabilityEntity.setPermission("permission-for-dummy");
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(dummyCapabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(userCapabilityEntity(userId, dummyCapabilityEntity.getId()));
    var capabilitiesIds = List.of(capabilityEntity.getId(), dummyCapabilityEntity.getId());
    var capabilitySetEntity = capabilitySetEntity(null, capabilitiesIds);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);
    var userCapabilitySetEntity = userCapabilitySetEntity(userId, capabilitySetEntity.getId());
    entityManager.persistAndFlush(userCapabilitySetEntity);

    var permissions = capabilityRepository
      .findPermissionsByPrefixesAndPermissionNames(userId,
        "{permission-not-for-dummy, permission-for-dummy}", "{permission}");
    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0)).isEqualTo("permission-not-for-dummy");
  }
}
