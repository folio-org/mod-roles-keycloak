package org.folio.roles.service.loadablerole;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.model.PageResult.empty;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowingConsumer;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.support.LoadablePermissionUtils;
import org.folio.test.types.UnitTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(InstancioExtension.class)
class LoadableRoleCapabilityAssignmentHelperTest {

  private static final UUID ROLE1_ID = randomUUID();
  private static final UUID ROLE2_ID = randomUUID();

  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityService capabilityService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;
  @InjectMocks
  private LoadableRoleCapabilityAssignmentHelper helper;

  @Test
  void assignCapabilitiesAndSets_positive() {
    var perm1 = loadablePermissionEntity(ROLE1_ID);
    var perm2 = loadablePermissionEntity(ROLE1_ID);
    var perm3 = loadablePermissionEntity(ROLE2_ID);
    var perm4 = loadablePermissionEntity(ROLE2_ID);

    var cap1 = capability(randomUUID(), perm1.getPermissionName());
    var capSet1 = capabilitySet(randomUUID()).permission(perm2.getPermissionName());
    var cap2 = capability(randomUUID(), perm3.getPermissionName());
    var capSet2 = capabilitySet(randomUUID()).permission(perm4.getPermissionName());

    mockAssignmentCalls(perm1, perm2, cap1, capSet1);
    mockAssignmentCalls(perm3, perm4, cap2, capSet2);

    var assigned = helper.assignCapabilitiesAndSetsForPermissions(List.of(perm1, perm2, perm3, perm4));

    assertThat(assigned).satisfiesExactlyInAnyOrder(
      permissionWithCapability(perm1, cap1),
      permissionWithCapabilitySet(perm2, capSet1),
      permissionWithCapability(perm3, cap2),
      permissionWithCapabilitySet(perm4, capSet2));
  }

  @Test
  void assignCapabilitiesAndSets_positive_noCapabilitiesAssigned() {
    var perm1 = loadablePermissionEntity(ROLE1_ID);
    var perm2 = loadablePermissionEntity(ROLE1_ID);
    var perm3 = loadablePermissionEntity(ROLE2_ID);
    var perm4 = loadablePermissionEntity(ROLE2_ID);

    mockAssignmentCalls(perm1, perm2, null, null);
    mockAssignmentCalls(perm3, perm4, null, null);

    var assigned = helper.assignCapabilitiesAndSetsForPermissions(List.of(perm1, perm2, perm3, perm4));

    assertThat(assigned).isEmpty();
  }

  @Test
  void assignCapabilitiesAndSets_negative_permissionRoleIdIsNull() {
    var perm1 = loadablePermissionEntity(null);

    assertThatThrownBy(() -> helper.assignCapabilitiesAndSetsForPermissions(List.of(perm1)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("element cannot be mapped to a null key");
  }

  @Test
  void removeCapabilitiesAndSets_positive() {
    var perm1 = loadablePermissionEntity(ROLE1_ID, randomUUID(), null);
    var perm2 = loadablePermissionEntity(ROLE1_ID, null, randomUUID());
    var perm3 = loadablePermissionEntity(ROLE2_ID, randomUUID(), null);
    var perm4 = loadablePermissionEntity(ROLE2_ID, null, randomUUID());

    moveRemovalCalls(perm1, perm2);
    moveRemovalCalls(perm3, perm4);

    var removed = helper.removeCapabilitiesAndSetsForPermissions(List.of(perm1, perm2, perm3, perm4));

    assertThat(removed).satisfiesExactlyInAnyOrder(
      permissionWithNoCapability(perm1),
      permissionWithNoCapabilitySet(perm2),
      permissionWithNoCapability(perm3),
      permissionWithNoCapabilitySet(perm4));
  }

  @Test
  void removeCapabilitiesAndSets_positive_noCapabilitiesRemoved() {
    var perm1 = loadablePermissionEntity(ROLE1_ID);
    var perm2 = loadablePermissionEntity(ROLE1_ID);
    var perm3 = loadablePermissionEntity(ROLE2_ID);
    var perm4 = loadablePermissionEntity(ROLE2_ID);

    moveRemovalCalls(perm1, perm2);
    moveRemovalCalls(perm3, perm4);

    var removed = helper.removeCapabilitiesAndSetsForPermissions(List.of(perm1, perm2, perm3, perm4));

    assertThat(removed).isEmpty();
  }

  @Test
  void removeCapabilitiesAndSets_negative_permissionRoleIdIsNull() {
    var perm1 = loadablePermissionEntity(null);

    assertThatThrownBy(() -> helper.removeCapabilitiesAndSetsForPermissions(List.of(perm1)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("element cannot be mapped to a null key");
  }

  private static LoadablePermissionEntity loadablePermissionEntity(UUID roleId) {
    return loadablePermissionEntity(roleId, null, null);
  }

  private static LoadablePermissionEntity loadablePermissionEntity(UUID roleId, UUID capabilityId,
    UUID capabilitySetId) {
    return LoadablePermissionUtils.loadablePermissionEntity(roleId,
      loadablePermission().capabilityId(capabilityId).capabilitySetId(capabilitySetId));
  }

  private void moveRemovalCalls(LoadablePermissionEntity perm1, LoadablePermissionEntity perm2) {
    doNothing().when(roleCapabilityService).delete(perm1.getRoleId(),
      perm1.getCapabilityId() != null ? List.of(perm1.getCapabilityId()) : emptyList());
    doNothing().when(roleCapabilitySetService).delete(perm2.getRoleId(),
      perm2.getCapabilitySetId() != null ? List.of(perm2.getCapabilitySetId()) : emptyList());
  }

  private static ThrowingConsumer<LoadablePermissionEntity> permissionWithCapabilitySet(LoadablePermissionEntity perm,
    CapabilitySet assignedCapabilitySet) {
    return permissionEntity -> {
      assertThat(permissionEntity.getId()).isEqualTo(perm.getId());
      assertThat(permissionEntity.getCapabilitySetId()).isEqualTo(assignedCapabilitySet.getId());
    };
  }

  private static ThrowingConsumer<LoadablePermissionEntity> permissionWithCapability(LoadablePermissionEntity perm,
    Capability assignedCapability) {
    return permissionEntity -> {
      assertThat(permissionEntity.getId()).isEqualTo(perm.getId());
      assertThat(permissionEntity.getCapabilityId()).isEqualTo(assignedCapability.getId());
    };
  }

  private static ThrowingConsumer<LoadablePermissionEntity> permissionWithNoCapabilitySet(
    LoadablePermissionEntity perm) {
    return permissionEntity -> {
      assertThat(permissionEntity.getId()).isEqualTo(perm.getId());
      assertThat(permissionEntity.getCapabilitySetId()).isNull();
    };
  }

  private static ThrowingConsumer<LoadablePermissionEntity> permissionWithNoCapability(LoadablePermissionEntity perm) {
    return permissionEntity -> {
      assertThat(permissionEntity.getId()).isEqualTo(perm.getId());
      assertThat(permissionEntity.getCapabilityId()).isNull();
    };
  }
  
  private void mockAssignmentCalls(LoadablePermissionEntity perm1, LoadablePermissionEntity perm2, Capability cap,
    CapabilitySet capSet) {
    when(capabilityService.findByPermissionNames(Set.of(perm1.getPermissionName(), perm2.getPermissionName())))
      .thenReturn(cap != null ? List.of(cap) : emptyList());
    if (cap != null) {
      when(roleCapabilityService.create(perm1.getRoleId(), List.of(cap.getId())))
        .thenReturn(empty()); // response is not used, can return empty()
    }

    when(capabilitySetService.findByPermissionNames(Set.of(perm1.getPermissionName(), perm2.getPermissionName())))
      .thenReturn(capSet != null ? List.of(capSet) : emptyList());
    if (capSet != null) {
      when(roleCapabilitySetService.create(perm1.getRoleId(), List.of(capSet.getId())))
        .thenReturn(empty()); // response is not used, can return empty()
    }
  }
}
