package org.folio.roles.service.loadablerole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntities;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(InstancioExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class LoadablePermissionServiceTest {

  @InjectMocks private LoadablePermissionService service;
  @Mock private LoadablePermissionRepository repository;
  @Mock private LoadableRoleMapper mapper;
  @Mock private LoadableRoleCapabilityAssignmentHelper assignmentHelper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void findAllByPermissions_positive() {
    var perms = loadablePermissions(5);
    var permEntities = loadablePermissionEntities(perms);
    var permNames = mapItems(perms, LoadablePermission::getPermissionName);

    when(repository.findAllByPermissionNameIn(permNames)).thenReturn(permEntities);
    when(mapper.toPermission(permEntities)).thenReturn(perms);

    var actual = service.findAllByPermissions(permNames);

    assertThat(actual).isEqualTo(perms);
  }

  @Test
  void findAllByPermissions_positive_samePermissionForMultipleRoles(CapturedOutput output) {
    var permissionName = "inventory-storage.locations.collection.get";
    var roleId1 = UUID.randomUUID();
    var roleId2 = UUID.randomUUID();
    var perm1 = loadablePermission(roleId1, permissionName);
    var perm2 = loadablePermission(roleId2, permissionName);
    var perms = List.of(perm1, perm2);
    var permEntities = loadablePermissionEntities(perms);

    when(repository.findAllByPermissionNameIn(List.of(permissionName))).thenReturn(permEntities);
    when(mapper.toPermission(permEntities)).thenReturn(perms);

    var actual = service.findAllByPermissions(List.of(permissionName));

    assertThat(actual).hasSize(2)
      .extracting(LoadablePermission::getRoleId)
      .containsExactlyInAnyOrder(roleId1, roleId2);
    assertThat(output.getAll()).contains("totalRecords = 2")
      .contains(roleId1.toString())
      .contains(roleId2.toString());
  }

  @Test
  void save_positive() {
    var perm = loadablePermission();
    var permEntity = loadablePermissionEntity(perm.getRoleId(), perm);

    when(mapper.toPermissionEntity(perm)).thenReturn(permEntity);
    when(repository.save(permEntity)).thenReturn(permEntity);
    when(mapper.toPermission(permEntity)).thenReturn(perm);

    var actual = service.save(perm);

    assertThat(actual).isEqualTo(perm);
  }

  @Test
  void saveAll_positive() {
    var perms = loadablePermissions(5);
    var permEntities = loadablePermissionEntities(perms);

    when(mapper.toPermissionEntity(perms)).thenReturn(permEntities);
    when(repository.saveAll(permEntities)).thenReturn(permEntities);
    when(mapper.toPermission(permEntities)).thenReturn(perms);

    var actual = service.saveAll(perms);

    assertThat(actual).isEqualTo(perms);
  }

  @Test
  void assignCapabilitiesAndSets_positive() {
    var permissions = loadablePermissions(2);
    var permissionEntities = loadablePermissionEntities(permissions);
    var permissionKeys = mapItems(permissionEntities, entity -> LoadablePermissionKey.of(entity.getRoleId(),
      entity.getPermissionName()));

    when(repository.findAllById(permissionKeys)).thenReturn(permissionEntities);
    when(assignmentHelper.assignCapabilitiesAndSetsForPermissions(permissionEntities)).thenReturn(
      Set.of(permissionEntities.getFirst()));

    service.assignCapabilitiesAndSets(permissionKeys);

    verify(assignmentHelper).assignCapabilitiesAndSetsForPermissions(permissionEntities);
  }

  @Test
  void assignCapabilitiesAndSets_positive_emptyInput() {
    service.assignCapabilitiesAndSets(List.of());

    verifyNoInteractions(repository, assignmentHelper);
  }
}
