package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadableRoleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoadablePermissionRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private LoadablePermissionRepository loadablePermissionRepository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var perm = loadablePermission();
    perm.setMetadata(null);

    var roleId = UUID.randomUUID();
    var entity = loadablePermissionEntity(roleId, perm);
    var now = OffsetDateTime.now();

    loadablePermissionRepository.save(entity);

    var stored = entityManager.find(LoadablePermissionEntity.class, LoadablePermissionKey.of(roleId,
      perm.getPermissionName()));
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findAllByCapabilityId_excludeDummy() {
    var capabilityEntity = capabilityEntity(null);
    entityManager.persistAndFlush(capabilityEntity);
    var perm = loadablePermission();
    perm.setMetadata(null);
    var loadableRole = loadableRoleEntity();
    entityManager.persistAndFlush(loadableRole);
    var loadablePermissionEntity = loadablePermissionEntity(loadableRole.getId(), perm);
    loadablePermissionEntity.setCapabilityId(capabilityEntity.getId());
    loadablePermissionEntity.setCapabilitySetId(null);
    entityManager.persistAndFlush(loadablePermissionEntity);

    var loadablePermissions = loadablePermissionRepository
      .findAllByCapabilityId(capabilityEntity.getId())
      .toList();
    assertThat(loadablePermissions).hasSize(1);

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    loadablePermissions = loadablePermissionRepository
      .findAllByCapabilityId(capabilityEntity.getId())
      .toList();
    assertThat(loadablePermissions).isEmpty();
  }

  @Test
  void findAllByCapabilitySetId_excludeDummy() {
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    entityManager.persistAndFlush(capabilitySetEntity);
    var perm = loadablePermission();
    perm.setMetadata(null);
    var loadableRole = loadableRoleEntity();
    entityManager.persistAndFlush(loadableRole);
    var loadablePermissionEntity = loadablePermissionEntity(loadableRole.getId(), perm);
    loadablePermissionEntity.setCapabilityId(null);
    loadablePermissionEntity.setCapabilitySetId(capabilitySetEntity.getId());
    entityManager.persistAndFlush(loadablePermissionEntity);

    var loadablePermissions = loadablePermissionRepository
      .findAllByCapabilitySetId(capabilitySetEntity.getId())
      .toList();
    assertThat(loadablePermissions).hasSize(1);

    capabilitySetEntity.setDummyCapability(true);
    entityManager.flush();
    loadablePermissions = loadablePermissionRepository
      .findAllByCapabilitySetId(capabilitySetEntity.getId())
      .toList();
    assertThat(loadablePermissions).isEmpty();
  }
}
