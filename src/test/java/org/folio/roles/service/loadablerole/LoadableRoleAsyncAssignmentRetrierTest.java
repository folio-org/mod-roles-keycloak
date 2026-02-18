package org.folio.roles.service.loadablerole;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
class LoadableRoleAsyncAssignmentRetrierTest {

  private static final String TEST_ROLE_NAME = "test-async-role";

  @InjectMocks private LoadableRoleAsyncAssignmentRetrier asyncRetrier;

  @Mock private LoadableRoleAssignmentRetrier retrier;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_positive_delegatesToRetrier() {
    var roleId = randomUUID();

    doNothing().when(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    asyncRetrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_catchesAndLogsException() {
    var roleId = randomUUID();
    var expectedException = new RuntimeException("Assignment failed");

    doThrow(expectedException).when(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    asyncRetrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_catchesUnassignedPermissionsException() {
    var roleId = randomUUID();
    var unassignedPermissionsException =
      new RuntimeException("Unassigned permissions still exist for loadable role: " + TEST_ROLE_NAME);

    doThrow(unassignedPermissionsException).when(retrier)
      .retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    asyncRetrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_positive_handlesNullRoleName() {
    var roleId = randomUUID();
    String nullRoleName = null;

    doNothing().when(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, nullRoleName);

    asyncRetrier.retryAssignCapabilitiesAndSetsForPermissions(roleId, nullRoleName);

    verify(retrier).retryAssignCapabilitiesAndSetsForPermissions(roleId, nullRoleName);
  }
}
