package org.folio.roles.service.loadablerole;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.configuration.property.LoadableRoleRetryProperties;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.exception.UnassignedPermissionsException;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LoadableRoleAssignmentRetrierTest {

  private static final String TEST_ROLE_NAME = "test-role";
  private static final String ERROR_MESSAGE_UNASSIGNED =
    "Unassigned permissions still exist for loadable role: " + TEST_ROLE_NAME;
  private static final String ERROR_MESSAGE_DATABASE = "Database error";

  @InjectMocks private LoadableRoleAssignmentRetrier retrier;
  @Mock private LoadablePermissionRepository loadablePermissionRepository;
  @Mock private LoadableRoleCapabilityAssignmentHelper loadableRoleCapabilityAssignmentHelper;
  @Mock private LoadableRoleRetryProperties retryProperties;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_positive_allPermissionsAssigned() {
    var roleId = randomUUID();
    var permissions = createPermissionsWithAssignments(roleId);

    when(loadablePermissionRepository.findAllPermissionsWhereCapabilityExistByRoleId(roleId))
      .thenReturn(permissions);
    when(loadableRoleCapabilityAssignmentHelper.assignCapabilitiesAndSetsForPermissions(permissions))
      .thenReturn(Set.of());
    when(loadablePermissionRepository.saveAllAndFlush(permissions)).thenReturn(permissions);
    when(loadablePermissionRepository.existsByRoleIdAndCapabilityIdIsNull(roleId)).thenReturn(false);

    retrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(loadablePermissionRepository).saveAllAndFlush(permissions);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_positive_noPermissionsToAssign() {
    var roleId = randomUUID();

    when(loadablePermissionRepository.findAllPermissionsWhereCapabilityExistByRoleId(roleId))
      .thenReturn(List.of());
    when(loadablePermissionRepository.existsByRoleIdAndCapabilityIdIsNull(roleId)).thenReturn(false);

    retrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(loadableRoleCapabilityAssignmentHelper, never()).assignCapabilitiesAndSetsForPermissions(any());
    verify(loadablePermissionRepository, never()).saveAllAndFlush(any());
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_unassignedPermissionsStillExist() {
    var roleId = randomUUID();
    var permissions = createPermissionsWithAssignments(roleId);

    when(loadablePermissionRepository.findAllPermissionsWhereCapabilityExistByRoleId(roleId))
      .thenReturn(permissions);
    when(loadableRoleCapabilityAssignmentHelper.assignCapabilitiesAndSetsForPermissions(permissions))
      .thenReturn(Set.of());
    when(loadablePermissionRepository.saveAllAndFlush(permissions)).thenReturn(permissions);
    when(loadablePermissionRepository.existsByRoleIdAndCapabilityIdIsNull(roleId)).thenReturn(true);

    assertThatThrownBy(() -> retrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME))
      .isInstanceOf(UnassignedPermissionsException.class)
      .hasMessage(ERROR_MESSAGE_UNASSIGNED);

    verify(loadablePermissionRepository).saveAllAndFlush(permissions);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_repositoryThrowsException() {
    var roleId = randomUUID();
    var expectedException = new RuntimeException(ERROR_MESSAGE_DATABASE);

    when(loadablePermissionRepository.findAllPermissionsWhereCapabilityExistByRoleId(roleId))
      .thenThrow(expectedException);

    assertThatThrownBy(() -> retrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(ERROR_MESSAGE_DATABASE);

    verify(loadableRoleCapabilityAssignmentHelper, never()).assignCapabilitiesAndSetsForPermissions(any());
  }

  private static List<LoadablePermissionEntity> createPermissionsWithAssignments(UUID roleId) {
    var perm1 = loadablePermissionEntity(roleId, randomUUID(), null);
    var perm2 = loadablePermissionEntity(roleId, null, randomUUID());
    return List.of(perm1, perm2);
  }
}
