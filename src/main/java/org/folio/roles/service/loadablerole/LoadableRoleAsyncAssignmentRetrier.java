package org.folio.roles.service.loadablerole;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AllArgsConstructor
public class LoadableRoleAsyncAssignmentRetrier {

  private final LoadableRoleAssignmentRetrier retrier;

  @Async("executorForLoadableRolesAssignmentRetry")
  public void retryAssignCapabilitiesAndSetsForPermissions(UUID loadableRoleId, String loadableRoleName)  {
    try {
      retrier.retryAssignCapabilitiesAndSetsForPermissions(loadableRoleId, loadableRoleName);
    } catch (Exception exception) {
      log.warn("Failed to assign capabilities and "
          + "capability sets after all retry attempts: roleId = {}, roleName = {}, error = {}",
        loadableRoleId, loadableRoleName, exception.getMessage());
    }
  }
}
