package org.folio.roles.service.loadablerole;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.exception.UnassignedPermissionsException;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@AllArgsConstructor
public class LoadableRoleAssignmentRetrier {

  private final LoadablePermissionRepository loadablePermissionRepository;
  private final LoadableRoleCapabilityAssignmentHelper loadableRoleCapabilityAssignmentHelper;

  @Retryable(
    maxRetriesString = "#{@loadableRoleRetryProperties.maxAttempts}",
    delayString = "#{@loadableRoleRetryProperties.backoff.delayMs}"
  )
  @Transactional
  public void retryAssignCapabilitiesAndSetsForPermissions(UUID loadableRoleId, String loadableRoleName)  {
    log.info("Retrying assignment of capabilities and capability sets for loadable role: roleName = {}",
      loadableRoleName);
    if (!loadablePermissionRepository.existsByRoleId(loadableRoleId)) {
      throw new ServiceException("Loadable permissions not found in DB for loadable role: "
        + "roleName = " + loadableRoleName);
    }
    var permissionsNotAssigned = loadablePermissionRepository
      .findAllPermissionsWhereCapabilityExistByRoleId(loadableRoleId);
    if (isNotEmpty(permissionsNotAssigned)) {
      loadableRoleCapabilityAssignmentHelper.assignCapabilitiesAndSetsForPermissionsCommitted(permissionsNotAssigned);
      log.info("Assigned capabilities and capabilities set by permissions: roleName = {}, permissions [{}]",
        loadableRoleName, getPermissionNamesAsStr(permissionsNotAssigned));
    }
    if (loadablePermissionRepository.existsByRoleIdAndCapabilityIdIsNull(loadableRoleId)) {
      throw new UnassignedPermissionsException(
        String.format("Unassigned permissions still exist for loadable role: %s",
          loadableRoleName));
    }
    log.info("Complete all assignment of capabilities and capability sets within retry: roleName = {}",
      loadableRoleName);
  }

  private static String getPermissionNamesAsStr(Collection<LoadablePermissionEntity> permissions) {
    return permissions.stream()
      .map(LoadablePermissionEntity::getPermissionName)
      .collect(joining(", "));
  }
}
