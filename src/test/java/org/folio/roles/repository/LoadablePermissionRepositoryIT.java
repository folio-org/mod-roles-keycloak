package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadableRoleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
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
    perm.setCapabilityId(null);
    perm.setCapabilitySetId(null);

    var roleId = UUID.randomUUID();
    var loadableRole = loadableRoleEntity();
    loadableRole.setId(roleId);
    entityManager.persistAndFlush(loadableRole);
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
  void save_positive_logsPersistedEntity(CapturedOutput output) {
    var perm = loadablePermission();
    perm.setMetadata(null);
    perm.setCapabilityId(null);
    perm.setCapabilitySetId(null);

    var loadableRole = loadableRoleEntity();
    entityManager.persistAndFlush(loadableRole);
    var entity = loadablePermissionEntity(loadableRole.getId(), perm);

    loadablePermissionRepository.save(entity);
    entityManager.flush();

    assertThat(output).contains("Saved role_loadable_permission record: roleId=" + entity.getRoleId()
      + ", permissionName=" + entity.getPermissionName());
  }

  @Test
  void update_positive_logsUpdatedEntity(CapturedOutput output) {
    var perm = loadablePermission();
    perm.setMetadata(null);
    perm.setCapabilityId(null);
    perm.setCapabilitySetId(null);
    var loadableRole = loadableRoleEntity();
    entityManager.persistAndFlush(loadableRole);
    var roleId = loadableRole.getId();
    var entity = loadablePermissionEntity(roleId, perm);

    loadablePermissionRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    var stored = loadablePermissionRepository.findById(LoadablePermissionKey.of(roleId, perm.getPermissionName()))
      .orElseThrow();
    var capability = capabilityEntity(null);
    entityManager.persistAndFlush(capability);
    stored.setCapabilityId(capability.getId());

    loadablePermissionRepository.save(stored);
    entityManager.flush();

    assertThat(output).contains("Updated role_loadable_permission record: roleId=" + roleId
      + ", permissionName=" + perm.getPermissionName() + ", capabilityId=" + capability.getId());
  }

  @Test
  void findAllByPermissionNameIn_positive_samePermissionForMultipleRoles() {
    var permissionName = "inventory-storage.locations.collection.get";
    var role1 = loadableRoleEntity();
    var role2 = loadableRoleEntity();
    entityManager.persistAndFlush(role1);
    entityManager.persistAndFlush(role2);

    var perm1 = loadablePermission(role1.getId(), permissionName);
    perm1.setMetadata(null);
    perm1.setCapabilityId(null);
    perm1.setCapabilitySetId(null);

    var perm2 = loadablePermission(role2.getId(), permissionName);
    perm2.setMetadata(null);
    perm2.setCapabilityId(null);
    perm2.setCapabilitySetId(null);

    entityManager.persistAndFlush(loadablePermissionEntity(role1.getId(), perm1));
    entityManager.persistAndFlush(loadablePermissionEntity(role2.getId(), perm2));
    entityManager.clear();

    var actual = loadablePermissionRepository.findAllByPermissionNameIn(List.of(permissionName));

    assertThat(actual).hasSize(2);
    assertThat(actual.stream().map(LoadablePermissionEntity::getRoleId).toList())
      .containsExactlyInAnyOrder(role1.getId(), role2.getId());
    assertThat(actual.stream().map(LoadablePermissionEntity::getPermissionName).toList())
      .containsOnly(permissionName);
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
}
