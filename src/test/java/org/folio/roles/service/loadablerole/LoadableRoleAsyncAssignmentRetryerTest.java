package org.folio.roles.service.loadablerole;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@UnitTest
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LoadableRoleAsyncAssignmentRetryerTest {

  private static final String TEST_ROLE_NAME = "test-async-role";

  @InjectMocks private LoadableRoleAsyncAssignmentRetryer asyncRetryer;

  @Mock private LoadableRoleAssignmentRetryer retryer;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_positive_delegatesToRetryer() {
    var roleId = randomUUID();

    asyncRetryer.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    verify(retryer).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_catchesException(CapturedOutput output) {
    var roleId = randomUUID();
    var expectedException = new RuntimeException("Assignment failed");

    doThrow(expectedException).when(retryer).retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    asyncRetryer.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    assertThat(output.getAll())
      .contains("Failed to assign capabilities and capability sets after all retry attempts")
      .contains("roleId = " + roleId)
      .contains("roleName = " + TEST_ROLE_NAME)
      .contains("error = Assignment failed");
  }

  @Test
  void retryAssignCapabilitiesAndSetsForPermissions_negative_catchesUnassignedPermissionsException(
    CapturedOutput output) {
    var roleId = randomUUID();
    var unassignedPermissionsException =
      new RuntimeException("Unassigned permissions still exist for loadable role: " + TEST_ROLE_NAME);

    doThrow(unassignedPermissionsException).when(retryer)
      .retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    asyncRetryer.retryAssignCapabilitiesAndSetsForPermissions(roleId, TEST_ROLE_NAME);

    assertThat(output.getAll())
      .contains("Failed to assign capabilities and capability sets after all retry attempts")
      .contains("roleId = " + roleId)
      .contains("roleName = " + TEST_ROLE_NAME)
      .contains("error = Unassigned permissions still exist for loadable role: " + TEST_ROLE_NAME);
  }
}
